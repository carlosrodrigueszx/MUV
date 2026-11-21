package com.muv.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.muv.backend.service.PercursoService.*;

class PercursoServiceTest {
    final LocalDateTime partida = LocalDateTime.of(2026, 11, 20, 23, 50);

    @Test void calculaChegadasPorTrechoERespeitaHoraPlanejada() {
        var pontos = List.of(new Ponto("A", partida, null), new Ponto("1", null, null),
                new Ponto("2", partida.plusHours(3), null), new Ponto("B", null, null));
        var resultado = comHorarios(pontos, List.of(1200L, 2400L, 600L));
        assertEquals(partida.plusMinutes(20), resultado.get(1).horario());
        assertEquals(partida.plusHours(3), resultado.get(2).horario());
        assertEquals(partida.plusHours(3).plusMinutes(10), resultado.getLast().horario());
    }

    @Test void retomaPrevisaoDepoisDeTrechoIndisponivel() {
        var pontos = List.of(new Ponto("A", partida, null), new Ponto("1", null, null),
                new Ponto("2", null, null), new Ponto("3", partida.plusDays(1), null), new Ponto("B", null, null));
        var resultado = comHorarios(pontos, Arrays.asList(null, 300L, null, 600L));
        assertNull(resultado.get(1).horario());
        assertNull(resultado.get(2).horario());
        assertEquals(partida.plusDays(1).plusMinutes(10), resultado.getLast().horario());
    }
}
