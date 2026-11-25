package com.muv.backend.controller;

import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.EstimativaFreteService;
import com.muv.backend.service.PercursoService;
import com.muv.backend.service.PropostaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PropostaControllerTest {
    final AuthRepository contas = mock(AuthRepository.class);
    final PropostaService service = mock(PropostaService.class);
    final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PropostaController(
            contas, service, mock(PercursoService.class), mock(EstimativaFreteService.class))).build();
    final String url="/api/prestador/solicitacoes/12";

    @Test void exigeSessao() throws Exception {
        mvc.perform(get(url+"/avaliacao")).andExpect(status().isUnauthorized());
    }

    @Test void retornaCodigoETetoQuandoPropostaExcedeLimite() throws Exception {
        var conta = mock(AuthRepository.Account.class);
        when(conta.id()).thenReturn(7L);
        when(contas.findAccount("prestador")).thenReturn(Optional.of(conta));
        var erro = mock(PropostaService.LimitePropostaException.class);
        when(erro.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        when(erro.getReason()).thenReturn("Informe um valor de até R$ 180,00.");
        when(erro.codigo()).thenReturn("VALOR_ACIMA_DO_TETO");
        when(erro.teto()).thenReturn(new java.math.BigDecimal("180.00"));
        when(service.responder(eq(12L), eq(7L), any(RespostaPrestador.class))).thenThrow(erro);

        mvc.perform(post(url + "/propostas").principal(() -> "prestador").contentType("application/json")
                .content("{\"tipo\":\"AJUSTAR\",\"versaoEstimativa\":\"referencia-atual\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALOR_ACIMA_DO_TETO"))
                .andExpect(jsonPath("$.teto").value(180));
        verify(service).responder(eq(12L), eq(7L),
                argThat(resposta -> "referencia-atual".equals(resposta.versaoEstimativa())));
    }

    @Test void respondeComIdEConflito() throws Exception {
        var conta=mock(AuthRepository.Account.class);
        when(conta.id()).thenReturn(7L);
        when(contas.findAccount("prestador")).thenReturn(Optional.of(conta));
        when(service.responder(eq(12L),eq(7L),any(RespostaPrestador.class))).thenReturn(22L)
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));
        var envio=post(url+"/propostas").principal(()->"prestador").contentType("application/json").content("{\"tipo\":\"MANTER\"}");
        mvc.perform(envio).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(22));
        mvc.perform(envio).andExpect(status().isConflict());
        mvc.perform(post(url+"/propostas").contentType("application/json").content("{")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/prestador/solicitacoes/invalido/avaliacao")).andExpect(status().isBadRequest());
    }
}
