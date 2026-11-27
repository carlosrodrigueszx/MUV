package com.muv.backend.execucao;

import com.muv.backend.execucao.RegrasExecucao.Acao;
import com.muv.backend.execucao.RegrasExecucao.Contexto;
import com.muv.backend.service.NotificacaoInternaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransicaoFreteService {
    private final ExecucaoFreteRepository repositorio;
    private final ExecucaoFreteService acompanhamento;
    private final NotificacaoInternaService notificacoes;

    public TransicaoFreteService(ExecucaoFreteRepository repositorio, ExecucaoFreteService acompanhamento,
            NotificacaoInternaService notificacoes) {
        this.repositorio = repositorio;
        this.acompanhamento = acompanhamento;
        this.notificacoes = notificacoes;
    }

    public record Comando(
            UUID chaveOperacao, String estadoEsperado, Long revisaoDesignacao, Long cicloExecucaoId,
            String descricao, String justificativa, LocalDate data, LocalTime horario,
            Long interrupcaoId, Boolean confirmou, String situacaoEncomenda, String localOcorrencia) {
        public Comando {
            localOcorrencia = localOcorrencia == null || localOcorrencia.isBlank() ? null : localOcorrencia.strip();
        }

        String texto(Acao acao) {
            return acao == Acao.OCORRENCIA ? descricao : justificativa;
        }

        Long referencia(Acao acao) {
            return acao.usaCicloExecucao() ? cicloExecucaoId : interrupcaoId;
        }
        LocalDateTime dataHoraInformada() {
            return data == null || horario == null ? null : LocalDateTime.of(data, horario);
        }
    }

    public record Resultado(long eventoId, long autorId, String estadoAnterior, String estadoResultante) {}

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Resultado confirmar(long freteId, long autorId, Acao acao, Comando comando) {
        var frete = repositorio.bloquearFrete(freteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var vinculos = RegrasExecucao.vinculos(autorId, frete.prestadorId(), frete.contaMotorista());
        if (!vinculos.podeConfirmar(acao)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (comando.chaveOperacao() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a chave da confirmação.");
        }

        boolean inicio = acao == Acao.INICIO;
        if (inicio && (comando.data() != null || comando.horario() != null || comando.justificativa() != null
                || comando.descricao() != null || comando.interrupcaoId() != null || comando.cicloExecucaoId() != null
                || comando.confirmou() != null || comando.situacaoEncomenda() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados incompatíveis com o início.");
        }
        boolean exigeConfirmacao = acao == Acao.RETOMADA || acao == Acao.ENCERRAMENTO_SEM_CONCLUSAO;
        var recibo = repositorio.consultarRecibo(freteId, comando.chaveOperacao());
        if (recibo.isPresent()) {
            var original = recibo.get();
            if (original.acao() != acao || original.autorId() != autorId
                    || (exigeConfirmacao && !Boolean.TRUE.equals(comando.confirmou()))
                    || !Objects.equals(original.estadoAnterior(), comando.estadoEsperado())
                    || !Objects.equals(original.revisaoDesignacao(), comando.revisaoDesignacao())
                    || !Objects.equals(original.referenciaId(), comando.referencia(acao))
                    || !Objects.equals(original.texto(), comando.texto(acao))
                    || !Objects.equals(original.localOcorrencia(), comando.localOcorrencia())
                    || !Objects.equals(original.situacaoEncomenda(), comando.situacaoEncomenda())
                    || !Objects.equals(original.dataHoraInformada(), comando.dataHoraInformada())) {
                throw conflito("A chave já foi usada em outra confirmação.");
            }
            return new Resultado(original.eventoId(), original.autorId(),
                    original.estadoAnterior(), original.estadoResultante());
        }

        if (comando.estadoEsperado() == null || comando.revisaoDesignacao() == null
                || (!inicio && comando.referencia(acao) == null)
                || !RegrasExecucao.dadosValidos(acao, comando.texto(acao), comando.data(),
                        comando.horario(), Boolean.TRUE.equals(comando.confirmou()))
                || (!inicio && comando.horario().getNano() % 1_000 != 0)
                || (comando.localOcorrencia() != null && !acao.usaCicloExecucao())
                || (comando.situacaoEncomenda() != null
                    && (acao != Acao.ENCERRAMENTO_SEM_CONCLUSAO || !"ENCOMENDA".equals(frete.modalidade())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verifique os dados e a confirmação do formulário.");
        }

        Long referencia = inicio ? null : repositorio.referenciaVigente(freteId, acao).orElse(null);
        boolean parteDaInterrupcao = !acao.usaCicloExecucao();
        var vigente = new Contexto(frete.estado(), frete.revisaoDesignacao(),
                parteDaInterrupcao ? null : referencia, parteDaInterrupcao ? referencia : null);
        var informado = new Contexto(comando.estadoEsperado(), comando.revisaoDesignacao(),
                comando.cicloExecucaoId(), comando.interrupcaoId());
        boolean recursosValidos = (!inicio && acao != Acao.RETOMADA) || acompanhamento.recursosValidos(frete);
        if (!RegrasExecucao.novaConfirmacaoElegivel(acao, vinculos, vigente, informado, recursosValidos)
                || (acao != Acao.OCORRENCIA && !repositorio.atualizarEstado(frete, acao.estadoResultante()))) {
            throw conflito("O frete foi atualizado ou os recursos não são elegíveis. Recarregue o acompanhamento.");
        }

        long eventoId = repositorio.gravarEvento(frete, autorId, acao, comando.texto(acao));
        repositorio.gravarRecibo(freteId, acao, comando.chaveOperacao(), eventoId, vigente,
                referencia, comando.dataHoraInformada(), comando.situacaoEncomenda(), comando.localOcorrencia());
        notificarEnvolvidos(frete, acao);
        return new Resultado(eventoId, autorId, vigente.estado(), acao.estadoResultante());
    }

    private void notificarEnvolvidos(ExecucaoFreteRepository.Frete frete, Acao acao) {
        String titulo = switch (acao) {
            case INICIO -> "Frete iniciado";
            case OCORRENCIA -> "Ocorrência registrada";
            case INTERRUPCAO -> "Execução interrompida";
            case RETOMADA -> "Execução retomada";
            case ENCERRAMENTO_SEM_CONCLUSAO -> "Frete encerrado sem conclusão";
            default -> throw new IllegalArgumentException("Ação fora do acompanhamento do RF010.");
        };
        String mensagem = switch (acao) {
            case INICIO -> "A execução do frete foi iniciada.";
            case OCORRENCIA -> "Uma ocorrência foi registrada durante a execução. Consulte o histórico.";
            case INTERRUPCAO -> "A execução do frete foi interrompida. Consulte o histórico para detalhes.";
            case RETOMADA -> "A execução do frete foi retomada.";
            case ENCERRAMENTO_SEM_CONCLUSAO -> "O frete foi encerrado sem conclusão.";
            default -> throw new IllegalArgumentException("Ação fora do acompanhamento do RF010.");
        };
        notificacoes.notificar(frete.clienteId(), titulo, mensagem, frete.id(), frete.solicitacaoId());
        if (frete.prestadorId() != frete.clienteId()) {
            notificacoes.notificar(frete.prestadorId(), titulo, mensagem, frete.id(), frete.solicitacaoId());
        }
    }

    private static ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
