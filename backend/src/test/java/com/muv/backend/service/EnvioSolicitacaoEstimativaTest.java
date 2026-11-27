package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.EnvioSolicitacaoService.Endereco;
import com.muv.backend.service.EnvioSolicitacaoService.Envio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class EnvioSolicitacaoEstimativaTest {
    @Test
    void rejeitaOfertaAbaixoDoPisoAntesDeGravarOuNotificar() {
        var db = mock(JdbcTemplate.class);
        var contas = mock(AuthRepository.class);
        var notificacoes = mock(NotificacaoInternaService.class);
        var conta = mock(AuthRepository.Account.class);
        when(conta.id()).thenReturn(1L);
        when(contas.findAccount("cliente")).thenReturn(Optional.of(conta));
        when(db.query(anyString(), ArgumentMatchers.<RowMapper<Long>>any(), eq(1L))).thenReturn(List.of(1L));
        var service = new EnvioSolicitacaoService(db, contas, notificacoes);
        var endereco = new Endereco("60000-000", "Rua A", "1", "Centro", "Fortaleza", "CE");
        var envio = new Envio(UUID.randomUUID(), 2L, 3L, 4,
                LocalDateTime.now(ZoneId.of("America/Fortaleza")).plusDays(1), null,
                endereco, endereco, List.of(), new BigDecimal("39.99"), "Condições da oferta");
        var estimativa = new EstimativaFreteService.Estimativa(null, null, null, null, null,
                null, null, null, null, new BigDecimal("40.00"), null, null);

        var erro = assertThrows(ResponseStatusException.class, () -> service.enviar("cliente", envio, estimativa));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(erro.getReason()).contains("40,00");
        verify(db, never()).queryForObject(anyString(), eq(Long.class), any(Object[].class));
        verify(db, never()).update(anyString(), any(Object[].class));
        verifyNoInteractions(notificacoes);
    }
}
