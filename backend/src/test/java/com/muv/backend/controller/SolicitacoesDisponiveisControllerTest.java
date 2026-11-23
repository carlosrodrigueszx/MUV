package com.muv.backend.controller;

import com.muv.backend.repository.AuthRepository;
import com.muv.backend.repository.SolicitacoesDisponiveisRepository;
import com.muv.backend.service.SolicitacoesDisponiveisService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitacoesDisponiveisControllerTest {
    private final AuthRepository contas = mock(AuthRepository.class);
    private final SolicitacoesDisponiveisRepository repositorio = mock(SolicitacoesDisponiveisRepository.class);
    private final SolicitacoesDisponiveisController controller = new SolicitacoesDisponiveisController(contas, new SolicitacoesDisponiveisService(repositorio));
    private final Principal usuario = () -> "prestador@example.test";

    @Test void exigeSessaoEPerfilPrestador() {
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> controller.listar("TODAS", null, null, null, null)).getStatusCode().value());
        when(contas.findAccount(usuario.getName())).thenReturn(Optional.of(new AuthRepository.Account(12, "P", usuario.getName(), "hash")));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller.listar("TODAS", null, null, null, usuario)).getStatusCode().value());
        verify(repositorio, never()).listar(anyLong(), anyString(), any(), any(), any());
    }

    @Test void validaFiltrosEEncaminhaIdentidadeDaSessao() {
        when(contas.findAccount(usuario.getName())).thenReturn(Optional.of(new AuthRepository.Account(12, "P", usuario.getName(), "hash")));
        when(repositorio.existePrestador(12)).thenReturn(true);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.listar("INVALIDA", null, null, null, usuario)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.listar("TODAS", null, "CEARA", null, usuario)).getStatusCode().value());
        var data = LocalDate.of(2026, 11, 23);
        when(repositorio.listar(12, "PESSOA", "Crato", "CE", data)).thenReturn(List.of());
        assertTrue(controller.listar("PESSOA", "Crato", "CE", data, usuario).isEmpty());
        verify(repositorio).listar(12, "PESSOA", "Crato", "CE", data);
    }
}
