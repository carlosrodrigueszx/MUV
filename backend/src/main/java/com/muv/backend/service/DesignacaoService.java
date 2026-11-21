package com.muv.backend.service;

import com.muv.backend.dto.DesignacaoFrete.Consulta;
import com.muv.backend.dto.DesignacaoFrete.ConsultaCliente;
import com.muv.backend.dto.DesignacaoFrete.ComandoDesignacao;
import com.muv.backend.dto.DesignacaoFrete.ComandoSubstituicao;
import com.muv.backend.dto.DesignacaoFrete.Contexto;
import com.muv.backend.dto.DesignacaoFrete.DesignacaoAtual;
import com.muv.backend.dto.DesignacaoFrete.DesignacaoConfirmada;
import com.muv.backend.dto.DesignacaoFrete.SubstituicaoConfirmada;
import com.muv.backend.dto.DesignacaoFrete.AcordoRespondido;
import com.muv.backend.dto.DesignacaoFrete.VeiculoDisponivel;
import com.muv.backend.repository.DesignacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignacaoService {
    private final DesignacaoRepository repositorio;
    private final CompatibilidadeDesignacaoService compatibilidade;

    public DesignacaoService(
            DesignacaoRepository repositorio,
            CompatibilidadeDesignacaoService compatibilidade) {
        this.repositorio = repositorio;
        this.compatibilidade = compatibilidade;
    }

    @Transactional(readOnly = true)
    public Consulta consultar(long freteId, long prestadorId) {
        Contexto contexto = repositorio.buscarContexto(freteId, prestadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var veiculos = contexto.veiculos().stream().map(veiculo -> {
            var resultado = compatibilidade.avaliar(
                    prestadorId,
                    contexto.modalidade(),
                    contexto.quantidadePessoas(),
                    contexto.pesoEstimadoKg(),
                    veiculo);
            return new VeiculoDisponivel(
                    veiculo.id(),
                    veiculo.marca(),
                    veiculo.modelo(),
                    veiculo.ano(),
                    veiculo.placa(),
                    veiculo.cor(),
                    veiculo.modalidade(),
                    veiculo.ativo(),
                    veiculo.capacidadePassageiros(),
                    veiculo.capacidadeCargaKg(),
                    resultado.compativel(),
                    resultado.motivos());
        }).toList();

        return new Consulta(
                contexto.freteId(),
                contexto.solicitacaoId(),
                contexto.modalidade(),
                contexto.status(),
                contexto.dataPretendida(),
                contexto.origem(),
                contexto.destino(),
                contexto.valor(),
                contexto.condicoes(),
                contexto.revisao(),
                contexto.quantidadePessoas(),
                contexto.pesoEstimadoKg(),
                contexto.prestadorAutonomo(),
                contexto.designacaoAtual(),
                contexto.motoristas(),
                veiculos);
    }

    @Transactional(readOnly = true)
    public ConsultaCliente consultarCliente(long freteId, long clienteId) {
        return repositorio.buscarDesignacaoCliente(freteId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public DesignacaoConfirmada designar(
            long freteId,
            long prestadorId,
            ComandoDesignacao comando) {
        if (comando == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Motorista e veículo são obrigatórios.");
        }
        var frete = repositorio.bloquearFrete(freteId, prestadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (frete.prestadorAutonomo()) {
            throw regra("A identidade de motorista do autônomo ainda não está associada.");
        }
        if (comando.motoristaId() == null || comando.veiculoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Motorista e veículo são obrigatórios.");
        }
        if (!"AGUARDANDO_DESIGNACAO".equals(frete.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O frete não está aguardando designação.");
        }
        if (frete.revisao() != comando.revisao()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A designação foi atualizada. Consulte os dados novamente.");
        }
        if (!repositorio.motoristaVerificado(prestadorId, comando.motoristaId())) {
            throw regra("O motorista não possui vínculo verificado com o prestador.");
        }
        var veiculo = repositorio.buscarVeiculoParaValidacao(comando.veiculoId())
                .orElseThrow(() -> regra("O veículo informado não está disponível."));
        var resultado = compatibilidade.avaliar(
                prestadorId,
                frete.modalidade(),
                frete.quantidadePessoas(),
                frete.pesoEstimadoKg(),
                veiculo);
        if (!resultado.compativel()) {
            throw regra(String.join(",", resultado.motivos()));
        }
        long novaRevisao = repositorio.registrarDesignacao(
                freteId,
                prestadorId,
                comando.motoristaId(),
                comando.veiculoId(),
                comando.revisao());
        if (novaRevisao < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A designação foi atualizada. Consulte os dados novamente.");
        }
        return new DesignacaoConfirmada(
                freteId,
                "PRONTO_INICIO",
                novaRevisao,
                new DesignacaoAtual(
                        comando.motoristaId(),
                        null,
                        comando.veiculoId(),
                        veiculo.marca(),
                        veiculo.modelo(),
                        veiculo.placa()));
    }

    @Transactional
    public SubstituicaoConfirmada substituir(
            long freteId,
            long prestadorId,
            ComandoSubstituicao comando) {
        if (comando == null || comando.justificativa() == null
                || comando.justificativa().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A justificativa da substituição é obrigatória.");
        }
        var frete = repositorio.bloquearFrete(freteId, prestadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (frete.prestadorAutonomo()) {
            throw regra("A identidade de motorista do autônomo ainda não está associada.");
        }
        if (!"PRONTO_INICIO".equals(frete.status()) && !"INTERROMPIDO".equals(frete.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O frete não permite substituição no estado atual.");
        }
        if (frete.revisao() != comando.revisao()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A designação foi atualizada. Consulte os dados novamente.");
        }
        long motoristaId = comando.motoristaId() == null
                ? valorObrigatorio(frete.motoristaAtualId())
                : comando.motoristaId();
        long veiculoId = comando.veiculoId() == null
                ? valorObrigatorio(frete.veiculoAtualId())
                : comando.veiculoId();
        validarRecursos(prestadorId, frete, motoristaId, veiculoId);
        boolean alteraValor = comando.novoValor() != null
                && comando.novoValor().compareTo(frete.valorAtual()) != 0;
        String condicoesPropostas = comando.novasCondicoes() == null
                ? frete.condicoesAtuais()
                : comando.novasCondicoes().trim();
        boolean alteraCondicoes = comando.novasCondicoes() != null
                && !condicoesPropostas.equals(frete.condicoesAtuais());
        if (alteraValor || alteraCondicoes) {
            if (comando.novoValor() != null && comando.novoValor().signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O novo valor deve ser positivo.");
            }
            if (condicoesPropostas == null || condicoesPropostas.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As condições são obrigatórias.");
            }
            long acordoId = repositorio.registrarSolicitacaoAcordo(
                    freteId,
                    prestadorId,
                    frete.clienteId(),
                    motoristaId,
                    veiculoId,
                    comando.revisao(),
                    comando.novoValor() == null ? frete.valorAtual() : comando.novoValor(),
                    condicoesPropostas,
                    comando.justificativa().trim());
            var atual = repositorio.buscarVeiculoParaValidacao(frete.veiculoAtualId()).orElseThrow();
            return new SubstituicaoConfirmada(
                    freteId,
                    frete.status(),
                    frete.revisao(),
                    new DesignacaoAtual(frete.motoristaAtualId(), null, frete.veiculoAtualId(),
                            atual.marca(), atual.modelo(), atual.placa()),
                    acordoId);
        }
        if (motoristaId == frete.motoristaAtualId() && veiculoId == frete.veiculoAtualId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um recurso diferente.");
        }
        long novaRevisao = repositorio.registrarSubstituicao(
                freteId,
                prestadorId,
                frete.clienteId(),
                motoristaId,
                veiculoId,
                comando.revisao(),
                comando.justificativa().trim());
        if (novaRevisao < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        var veiculo = repositorio.buscarVeiculoParaValidacao(veiculoId).orElseThrow();
        return new SubstituicaoConfirmada(
                freteId,
                frete.status(),
                novaRevisao,
                new DesignacaoAtual(motoristaId, null, veiculoId,
                        veiculo.marca(), veiculo.modelo(), veiculo.placa()),
                null);
    }

    @Transactional
    public AcordoRespondido responderAcordo(
            long freteId,
            long acordoId,
            long clienteId,
            boolean aceitar) {
        var acordo = repositorio.bloquearAcordo(freteId, acordoId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"PENDENTE".equals(acordo.status())) {
            return new AcordoRespondido(
                    acordo.id(), acordo.status(), false, acordo.revisaoFrete());
        }
        if (!aceitar) {
            repositorio.rejeitarAcordo(acordoId, freteId, acordo.prestadorId());
            return new AcordoRespondido(acordoId, "REJEITADA", true, acordo.revisaoFrete());
        }
        if (acordo.revisaoOrigem() != acordo.revisaoFrete()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O frete foi atualizado depois da solicitação de acordo.");
        }
        if (!"PRONTO_INICIO".equals(acordo.freteStatus())
                && !"INTERROMPIDO".equals(acordo.freteStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        var frete = new com.muv.backend.repository.DesignacaoRepository.FreteBloqueado(
                freteId,
                acordo.freteStatus(),
                acordo.revisaoFrete(),
                acordo.modalidade(),
                acordo.quantidadePessoas(),
                acordo.pesoEstimadoKg(),
                false,
                acordo.novoMotoristaId(),
                acordo.novoVeiculoId(),
                clienteId,
                acordo.novoValor(),
                acordo.novasCondicoes());
        validarRecursos(acordo.prestadorId(), frete,
                acordo.novoMotoristaId(), acordo.novoVeiculoId());
        long revisao = repositorio.aplicarAcordo(freteId, acordo);
        if (revisao < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new AcordoRespondido(acordoId, "ACEITA", true, revisao);
    }

    private void validarRecursos(
            long prestadorId,
            com.muv.backend.repository.DesignacaoRepository.FreteBloqueado frete,
            long motoristaId,
            long veiculoId) {
        if (!repositorio.motoristaVerificado(prestadorId, motoristaId)) {
            throw regra("O motorista não possui vínculo verificado com o prestador.");
        }
        var veiculo = repositorio.buscarVeiculoParaValidacao(veiculoId)
                .orElseThrow(() -> regra("O veículo informado não está disponível."));
        var resultado = compatibilidade.avaliar(prestadorId, frete.modalidade(),
                frete.quantidadePessoas(), frete.pesoEstimadoKg(), veiculo);
        if (!resultado.compativel()) {
            throw regra(String.join(",", resultado.motivos()));
        }
    }

    private static long valorObrigatorio(Long valor) {
        if (valor == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O frete não possui designação completa.");
        }
        return valor;
    }

    private static ResponseStatusException regra(String mensagem) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, mensagem);
    }
}
