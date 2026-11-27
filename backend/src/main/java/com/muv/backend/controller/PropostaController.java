package com.muv.backend.controller;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.PropostaService;
import com.muv.backend.service.PercursoService;
import com.muv.backend.service.EstimativaFreteService;
import java.util.List;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prestador/solicitacoes/{id}")
public class PropostaController {
    private final AuthRepository contas;
    private final PropostaService servico;

    private final PercursoService percurso;
    private final EstimativaFreteService estimativas;

    public PropostaController(AuthRepository contas, PropostaService servico, PercursoService percurso, EstimativaFreteService estimativas) {
        this.percurso = percurso;
        this.estimativas = estimativas;
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping("/avaliacao")
    public AvaliacaoSolicitacao avaliar(@PathVariable long id, Principal principal) {
        return servico.avaliar(id, usuario(principal));
    }

    public record PercursoAvaliado(List<PercursoService.Ponto> pontos, List<PercursoService.Coordenada> geometria,
            List<Long> segundosPorTrecho, EstimativaFreteService.Estimativa estimativa,
            EstimativaFreteService.LimiteProposta limiteProposta) {}

    @GetMapping("/avaliacao/percurso")
    public PercursoAvaliado percurso(@PathVariable long id, Principal principal) {
        var avaliacao = servico.avaliar(id, usuario(principal));
        var rota = percurso.consultar(avaliacao);
        var distancia = avaliacao.percursoKm() == null ? rota.distanciaKm() : avaliacao.percursoKm();
        var estimativa = estimativas.calcular(avaliacao.modalidade(), distancia);
        return new PercursoAvaliado(rota.pontos(), rota.geometria(), rota.segundosPorTrecho(), estimativa, estimativas.limiteProposta(avaliacao, estimativa));
    }

    public record Confirmacao(long id) {}

    @PostMapping("/propostas")
    @ResponseStatus(HttpStatus.CREATED)
    public Confirmacao responder(@PathVariable long id, @RequestBody RespostaPrestador resposta, Principal principal) {
        return new Confirmacao(servico.responder(id, usuario(principal), resposta));
    }

    public record Recusa(String justificativa) {}

    @PostMapping("/recusa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recusar(@PathVariable long id, @RequestBody Recusa recusa, Principal principal) {
        servico.recusar(id, usuario(principal), recusa.justificativa());
    }

    @ExceptionHandler(PropostaService.LimitePropostaException.class)
    public ProblemDetail limiteRejeitado(PropostaService.LimitePropostaException erro) {
        var problema = ProblemDetail.forStatusAndDetail(erro.getStatusCode(), erro.getReason());
        problema.setProperty("codigo", erro.codigo());
        if (erro.teto() != null) problema.setProperty("teto", erro.teto());
        return problema;
    }

    private long usuario(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return contas.findAccount(principal.getName()).map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
