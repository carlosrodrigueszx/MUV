package com.muv.backend.execucao;

import static com.muv.backend.execucao.RegrasExecucao.*;
import static com.muv.backend.execucao.RegrasExecucao.Acao.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RegrasExecucaoTest {
    private final Vinculos motorista = vinculos(3, 2, 3L);
    private final Vinculos prestador = vinculos(2, 2, 3L);

    @Test
    void diferenciaOcorrenciaInterrupcaoRetomadaEEncerramento() {
        assertThat(INICIO.estadoResultante()).isEqualTo("EM_EXECUCAO");
        assertThat(OCORRENCIA.estadoResultante()).isEqualTo("EM_EXECUCAO");
        assertThat(INTERRUPCAO.estadoResultante()).isEqualTo("INTERROMPIDO");
        assertThat(RETOMADA.estadoResultante()).isEqualTo("EM_EXECUCAO");
        assertThat(ENCERRAMENTO_SEM_CONCLUSAO.estadoResultante()).isEqualTo("ENCERRADO_SEM_CONCLUSAO");
    }

    @Test
    void distinguePrestadorMotoristaEContasAlheiasEmTodosOsEstados() {
        var pares = Map.of(
                INICIO, new Contexto("PRONTO_INICIO", 1, null, null),
                OCORRENCIA, new Contexto("EM_EXECUCAO", 1, 16L, null),
                INTERRUPCAO, new Contexto("EM_EXECUCAO", 1, 16L, null),
                RETOMADA, new Contexto("INTERROMPIDO", 1, null, 17L),
                ENCERRAMENTO_SEM_CONCLUSAO, new Contexto("INTERROMPIDO", 1, null, 17L));
        for (Acao acao : Acao.values()) {
            for (Contexto contexto : pares.values()) {
                boolean estadoCorreto = contexto.equals(pares.get(acao));
                assertThat(novaConfirmacaoElegivel(acao, motorista, contexto, contexto, true))
                        .as("%s em %s para motorista", acao, contexto.estado()).isEqualTo(estadoCorreto);
                assertThat(novaConfirmacaoElegivel(acao, prestador, contexto, contexto, true))
                        .as("%s em %s para prestador", acao, contexto.estado())
                        .isEqualTo(estadoCorreto && acao != INICIO);
                assertThat(novaConfirmacaoElegivel(acao, vinculos(4, 2, 3L), contexto, contexto, true))
                        .isFalse();
            }
            for (String estado : new String[]{"AGUARDANDO_DESIGNACAO", "CANCELADO",
                    "CONCLUIDO", "ENCERRADO_SEM_CONCLUSAO"}) {
                var contexto = new Contexto(estado, 1, null, null);
                assertThat(novaConfirmacaoElegivel(acao, motorista, contexto, contexto, true)).isFalse();
            }
        }
    }

    @Test
    void rejeitaCicloAntigoMesmoComEstadoERevisaoIguais() {
        var formulario = new Contexto("EM_EXECUCAO", 2, 10L, null);
        var atual = new Contexto("EM_EXECUCAO", 2, 12L, null);
        assertThat(novaConfirmacaoElegivel(OCORRENCIA, motorista, atual, formulario, true)).isFalse();
        assertThat(novaConfirmacaoElegivel(INTERRUPCAO, prestador, atual, formulario, true)).isFalse();
        assertThat(novaConfirmacaoElegivel(OCORRENCIA, motorista, atual, atual, true)).isTrue();
    }

    @Test
    void rejeitaNovaInterrupcaoEFormularioDeOutraInterrupcao() {
        var antigo = new Contexto("INTERROMPIDO", 1, null, 10L);
        var atual = new Contexto("INTERROMPIDO", 1, null, 14L);
        assertThat(novaConfirmacaoElegivel(INTERRUPCAO, prestador, atual, atual, true)).isFalse();
        assertThat(novaConfirmacaoElegivel(RETOMADA, motorista, atual, antigo, true)).isFalse();
        assertThat(novaConfirmacaoElegivel(ENCERRAMENTO_SEM_CONCLUSAO, prestador, atual, antigo, true)).isFalse();
    }

    @Test
    void trataRecursosInvalidosEContextoLegadoSemEvento() {
        var pronto = new Contexto("PRONTO_INICIO", 1, null, null);
        var parado = new Contexto("INTERROMPIDO", 1, null, 15L);
        var legado = new Contexto("EM_EXECUCAO", 1, null, null);
        assertThat(acoesElegiveis(motorista, pronto, false)).isEmpty();
        assertThat(acoesElegiveis(motorista, parado, false)).containsExactly(ENCERRAMENTO_SEM_CONCLUSAO);
        assertThat(acoesElegiveis(motorista, legado, true)).isEmpty();
    }

    @Test
    void validaTextosEConfirmacoesSemLimitarDatas() {
        var dataPassada = LocalDate.of(2000, 1, 1);
        var dataFutura = LocalDate.of(2099, 1, 1);
        var hora = LocalTime.NOON;
        assertThat(dadosValidos(OCORRENCIA, "\u00a0\u2003", dataPassada, hora, false)).isFalse();
        assertThat(dadosValidos(INTERRUPCAO, "Motivo", dataFutura, null, false)).isFalse();
        assertThat(dadosValidos(RETOMADA, "Motivo", dataPassada, hora, false)).isFalse();
        assertThat(dadosValidos(ENCERRAMENTO_SEM_CONCLUSAO, "Motivo", dataFutura, hora, false)).isFalse();
        assertThat(dadosValidos(OCORRENCIA, "Relato", dataPassada, hora, false)).isTrue();
        assertThat(dadosValidos(RETOMADA, "Motivo", dataFutura, hora, true)).isTrue();
    }
}
