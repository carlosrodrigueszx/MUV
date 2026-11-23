package com.muv.backend.execucao;

import com.muv.backend.execucao.ExecucaoFreteRepository.Frete;
import com.muv.backend.execucao.RegrasExecucao.Acao;
import com.muv.backend.execucao.RegrasExecucao.Contexto;
import com.muv.backend.repository.DesignacaoRepository;
import com.muv.backend.service.CompatibilidadeDesignacaoService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExecucaoFreteService {
    private final ExecucaoFreteRepository repositorio;
    private final DesignacaoRepository designacoes;
    private final CompatibilidadeDesignacaoService compatibilidade;

    public ExecucaoFreteService(
            ExecucaoFreteRepository repositorio, DesignacaoRepository designacoes,
            CompatibilidadeDesignacaoService compatibilidade) {
        this.repositorio = repositorio;
        this.designacoes = designacoes;
        this.compatibilidade = compatibilidade;
    }

    public record Recursos(String motorista, String veiculo, String placa) {}
    public record Evento(long id, Acao tipo, String autor, String descricao, String justificativa,
                         OffsetDateTime horarioRegistro) {}
    public record Acompanhamento(
            long freteId, String modalidade, String estado, long revisaoDesignacao,
            Recursos recursos, Long cicloExecucaoId, Long interrupcaoId, List<Evento> eventos,
            List<Acao> acoesElegiveis, boolean prestadorResponsavel, String usuarioAutenticado) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Acompanhamento consultar(long freteId, long contaId, String nomeDaConta) {
        var frete = repositorio.consultarFrete(freteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var vinculos = RegrasExecucao.vinculos(contaId, frete.prestadorId(), frete.contaMotorista());
        if (!vinculos.podeConsultar()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var eventos = repositorio.consultarEventos(freteId);
        var ultimaTransicao = eventos.stream().filter(evento -> evento.tipo() != Acao.OCORRENCIA)
                .findFirst().orElse(null);
        Long ciclo = null;
        Long interrupcao = null;
        if (ultimaTransicao != null) {
            if ("EM_EXECUCAO".equals(frete.estado())
                    && (ultimaTransicao.tipo() == Acao.INICIO || ultimaTransicao.tipo() == Acao.RETOMADA)) {
                ciclo = ultimaTransicao.id();
            }
            if ("INTERROMPIDO".equals(frete.estado()) && ultimaTransicao.tipo() == Acao.INTERRUPCAO) {
                interrupcao = ultimaTransicao.id();
            }
        }
        var contexto = new Contexto(frete.estado(), frete.revisaoDesignacao(), ciclo, interrupcao);
        boolean exigeRecursos = "PRONTO_INICIO".equals(frete.estado()) || interrupcao != null;
        var acoes = RegrasExecucao.acoesElegiveis(vinculos, contexto, exigeRecursos && recursosValidos(frete));
        var recursos = frete.motoristaId() == null || frete.veiculoId() == null
                ? null : new Recursos(frete.motorista(), frete.veiculo(), frete.placa());
        return new Acompanhamento(frete.id(), frete.modalidade(), frete.estado(), frete.revisaoDesignacao(),
                recursos, ciclo, interrupcao, eventos, acoes, vinculos.prestadorResponsavel(), nomeDaConta);
    }

    boolean recursosValidos(Frete frete) {
        return frete.motoristaId() != null && frete.veiculoId() != null
                && designacoes.motoristaVerificado(frete.prestadorId(), frete.motoristaId())
                && designacoes.buscarVeiculoParaValidacao(frete.veiculoId())
                        .map(veiculo -> compatibilidade.avaliar(frete.prestadorId(), frete.modalidade(),
                                frete.quantidadePessoas(), frete.pesoEstimadoKg(), veiculo).compativel())
                        .orElse(false);
    }
}
