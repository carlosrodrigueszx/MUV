package com.muv.backend.execucao;

import com.muv.backend.service.AuthService;
import com.muv.backend.service.NotificacaoInternaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConclusaoFreteService {
    private static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");
    private final ExecucaoFreteRepository repositorio;
    private final NotificacaoInternaService notificacoes;

    public ConclusaoFreteService(ExecucaoFreteRepository repositorio, NotificacaoInternaService notificacoes) {
        this.repositorio = repositorio;
        this.notificacoes = notificacoes;
    }

    public record Comando(Long revisaoDesignacao, Long cicloExecucaoId, LocalDate data,
                          LocalTime horario, String nomeRecebedor, String cpfRecebedor) {}

    @Transactional
    public void concluir(long id, long autor, Comando comando) {
        var frete = repositorio.bloquearFrete(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!Objects.equals(frete.contaMotorista(), autor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (comando.revisaoDesignacao() == null || comando.cicloExecucaoId() == null) {
            throw invalido("Atualize o acompanhamento antes de confirmar a conclusão.");
        }

        boolean encomenda = "ENCOMENDA".equals(frete.modalidade());
        LocalDateTime agora = LocalDateTime.now(FORTALEZA).truncatedTo(ChronoUnit.MICROS);
        LocalDateTime realizadaEm = agora;
        String nome = null;
        String cpf = null;
        if (encomenda) {
            nome = comando.nomeRecebedor() == null ? "" : comando.nomeRecebedor().strip();
            if (comando.data() == null || comando.horario() == null) {
                throw invalido("Informe a data e o horário em que a entrega aconteceu.");
            }
            if (nome.isBlank() || nome.length() > 128) {
                throw invalido("Informe o nome de quem recebeu a encomenda, com até 128 caracteres.");
            }
            if (comando.cpfRecebedor() != null && !comando.cpfRecebedor().isBlank()) {
                cpf = comando.cpfRecebedor().strip().replace(".", "").replace("-", "");
                if (!cpf.matches("[0-9]{11}") || !AuthService.isValidCpf(cpf)) {
                    throw invalido("O CPF informado é inválido. Corrija o número ou deixe o campo em branco.");
                }
            }
            realizadaEm = LocalDateTime.of(comando.data(), comando.horario()).truncatedTo(ChronoUnit.MICROS);
            if (realizadaEm.isAfter(agora)) throw invalido("O horário da entrega deve estar no passado ou no presente.");
        } else if (comando.data() != null || comando.horario() != null
                || comando.nomeRecebedor() != null || comando.cpfRecebedor() != null) {
            throw invalido("Dados de entrega são exclusivos de fretes de encomendas.");
        }

        boolean mesmoContexto = frete.revisaoDesignacao() == comando.revisaoDesignacao()
                && repositorio.referenciaVigente(id, RegrasExecucao.Acao.OCORRENCIA)
                        .filter(comando.cicloExecucaoId()::equals).isPresent();
        if (mesmoContexto && "CONCLUIDO".equals(frete.estado())
                && repositorio.mesmaConclusao(id, autor, encomenda ? realizadaEm : null, nome, cpf)) {
            return;
        }
        if (!mesmoContexto || !"EM_EXECUCAO".equals(frete.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O frete ou o ciclo de execução mudou. Atualize o acompanhamento.");
        }
        repositorio.concluir(frete.id(), autor, realizadaEm, nome, cpf);
        repositorio.gravarEvento(frete, autor, RegrasExecucao.Acao.CONCLUSAO, null);
        notificacoes.notificar(frete.clienteId(), "Frete concluído",
                "A conclusão do seu frete foi registrada. Consulte os detalhes do evento no histórico.",
                frete.id(), frete.solicitacaoId());
    }

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
