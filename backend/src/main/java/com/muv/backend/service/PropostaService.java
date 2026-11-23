package com.muv.backend.service;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.repository.PropostaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class PropostaService {
    private final PropostaRepository propostas;
    private final NotificacaoInternaService notificacoes;

    public PropostaService(PropostaRepository propostas, NotificacaoInternaService notificacoes) {
        this.propostas = propostas;
        this.notificacoes = notificacoes;
    }

    public AvaliacaoSolicitacao avaliar(long solicitacao, long prestador) {
        return contexto(solicitacao, prestador, false).detalhe();
    }

    @Transactional
    public long responder(long solicitacao, long prestador, RespostaPrestador resposta) {
        var contexto = contexto(solicitacao, prestador, true);
        var oferta = contexto.detalhe();
        if (resposta == null || resposta.tipo() == null || resposta.chaveEnvio() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Identifique o tipo e a tentativa de envio.");
        }
        String condicoes = resposta.condicoesRevisadas();
        BigDecimal valor = resposta.valorRevisado();
        if (resposta.tipo() == RespostaPrestador.Tipo.AJUSTAR) {
            valor = resposta.novoValor();
            String mensagem = resposta.justificativa();
            if (mensagem == null || mensagem.isBlank() || mensagem.length() > 500) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe uma justificativa de até 500 caracteres.");
            }
            condicoes = condicoes == null || condicoes.isBlank() ? mensagem.strip()
                    : condicoes + "\n" + mensagem.strip();
        }
        if (valor == null || valor.signum() <= 0 || valor.scale() > 2
                || valor.compareTo(new BigDecimal("99999999999.99")) > 0 || condicoes == null || condicoes.isBlank()) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um valor positivo em reais e as condições da proposta.");
        }
        var anterior = propostas.envioAnterior(solicitacao, prestador, resposta.chaveEnvio());
        if (anterior.isPresent()) {
            var registro = anterior.get();
            if (valor.compareTo(registro.valor()) == 0 && condicoes.equals(registro.condicoes())) return registro.id();
            throw erro(HttpStatus.CONFLICT, "Esta tentativa já foi usada para outra resposta.");
        }
        exigirSemProposta(oferta);
        if (!"ABERTA".equals(oferta.status())) throw erro(HttpStatus.CONFLICT, "A solicitação está encerrada.");
        boolean mesmoValor = oferta.valorCliente() == null ? resposta.valorRevisado() == null
                : resposta.valorRevisado() != null && oferta.valorCliente().compareTo(resposta.valorRevisado()) == 0;
        if (!mesmoValor || !Objects.equals(oferta.condicoesCliente(), resposta.condicoesRevisadas())) {
            throw erro(HttpStatus.CONFLICT, "A oferta do cliente mudou. Abra novamente a avaliação.");
        }
        long id = propostas.registrar(solicitacao, prestador, valor, condicoes, resposta.chaveEnvio());
        notificacoes.notificar(contexto.clienteId(), "Nova proposta",
                "Você recebeu uma proposta para sua solicitação.", null, solicitacao);
        return id;
    }

    @Transactional
    public void recusar(long solicitacao, long prestador, String mensagem) {
        var atual = contexto(solicitacao, prestador, true);
        exigirSemProposta(atual.detalhe());
        if (!"ABERTA".equals(atual.detalhe().status())) throw erro(HttpStatus.CONFLICT, "A solicitação está encerrada.");
        if (mensagem == null || mensagem.isBlank()) throw erro(HttpStatus.BAD_REQUEST, "Informe o motivo da recusa.");
        propostas.recusar(solicitacao, prestador, mensagem.strip());
    }

    private PropostaRepository.Contexto contexto(long solicitacao, long prestador, boolean bloquear) {
        if (solicitacao <= 0) throw erro(HttpStatus.BAD_REQUEST, "Identificador de solicitação inválido.");
        return propostas.consultar(solicitacao, prestador, bloquear)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Solicitação indisponível para este prestador."));
    }

    private static void exigirSemProposta(AvaliacaoSolicitacao oferta) {
        if (oferta.propostaEnviada()) throw erro(HttpStatus.CONFLICT, "Você já enviou uma proposta para esta solicitação.");
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
