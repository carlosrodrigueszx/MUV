package com.muv.backend.controller;

import com.muv.backend.model.*;
import com.muv.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DriverLinkingControllerTest {
    DriverLinkingService service = mock(DriverLinkingService.class);
    DriverAccountService accounts = mock(DriverAccountService.class);
    MockMvc mvc;
    VinculoMotorista vinculo;

    @BeforeEach
    void setup() {
        var controller = new DriverLinkingController(mock(PrestadorConsultaService.class));
        ReflectionTestUtils.setField(controller, "driverLinkingService", service);
        ReflectionTestUtils.setField(controller, "driverAccounts", accounts);
        when(accounts.resposta(any(VinculoMotorista.class))).thenAnswer(i -> com.muv.backend.dto.MotoristaConsultaResponse.from((VinculoMotorista) i.getArgument(0)));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        Motorista m = new Motorista();
        m.setId(10L); m.setNome("Motorista Teste"); m.setTelefone("85900000000");
        m.setCnh("12345678900"); m.setCategoriaCnh("D");
        Prestador p = new Prestador(); p.setSenha("NAO_EXPOR");
        vinculo = new VinculoMotorista(); vinculo.setMotorista(m); vinculo.setPrestador(p);
    }

    @Test
    void listaSomenteDtoSemExporEntidadesOuSenha() throws Exception {
        when(service.listarMotoristasPorPrestador(1L)).thenReturn(List.of(vinculo));
        mvc.perform(get("/api/prestadores/1/motoristas").principal(() -> "gestor@example.test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Motorista Teste"))
                .andExpect(jsonPath("$[0].prestador").doesNotExist())
                .andExpect(jsonPath("$[0].senha").doesNotExist());
        verify(service).autorizar(1L, "gestor@example.test");
    }

    @Test
    void cadastraJsonComCamposEmPortugues() throws Exception {
        when(service.cadastrarEVincularMotorista(eq(1L), any())).thenReturn(vinculo);
        mvc.perform(post("/api/prestadores/1/motoristas").principal(() -> "gestor@example.test")
                .contentType("application/json").content("""
                {"nome":"Motorista Teste","telefone":"85900000000","cnh":"12345678900","categoriaCnh":"D","cpf":"12345678909","nascimento":"1990-01-01","genero":"masculino","email":"motorista@example.test","senha":"SenhaTeste!123","confirmarSenha":"SenhaTeste!123"}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statusVerificacao").value("PENDENTE"));
    }

    @Test
    void rejeitaCadastroInvalido() throws Exception {
        mvc.perform(post("/api/prestadores/1/motoristas").principal(() -> "gestor@example.test")
                .contentType("application/json").content("""
                {"nome":" ","telefone":"","cnh":"abc","categoriaCnh":"Z"}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        verify(service, never()).cadastrarEVincularMotorista(any(), any());
    }

    @Test
    void impedeConsultaDeOutraOrganizacao() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Outra organização"))
                .when(service).autorizar(2L, "gestor@example.test");
        mvc.perform(get("/api/prestadores/2/motoristas").principal(() -> "gestor@example.test"))
                .andExpect(status().isForbidden());
        verify(service, never()).listarMotoristasPorPrestador(any());
    }

    @Test
    void consultaAptosFiltraPendentesERejeitados() throws Exception {
        when(service.listarMotoristasPorPrestador(1L)).thenReturn(List.of(vinculo));
        mvc.perform(get("/api/prestadores/1/motoristas?aptos=true").principal(() -> "gestor@example.test"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void verificacaoUsaGestorDaSessaoENaoIdDoCliente() throws Exception {
        vinculo.setStatusVerificacao("VERIFICADO");
        when(service.atualizarVerificacao(1L, 10L, "VERIFICADO", 1L)).thenReturn(vinculo);
        mvc.perform(patch("/api/prestadores/1/motoristas/10/verificacao?gestorId=999")
                .principal(() -> "gestor@example.test").contentType("application/json")
                .content("{\"statusVerificacao\":\"VERIFICADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusVerificacao").value("VERIFICADO"));
        verify(service).atualizarVerificacao(1L, 10L, "VERIFICADO", 1L);
    }
}
