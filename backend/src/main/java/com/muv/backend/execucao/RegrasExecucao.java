package com.muv.backend.execucao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class RegrasExecucao {
    private static final java.util.regex.Pattern ESPACOS = java.util.regex.Pattern.compile("(?U)[\\s\\p{javaWhitespace}]*");

    private RegrasExecucao() {}

    public enum Acao {
        INICIO("PRONTO_INICIO", "EM_EXECUCAO"),
        OCORRENCIA("EM_EXECUCAO", "EM_EXECUCAO"),
        INTERRUPCAO("EM_EXECUCAO", "INTERROMPIDO"),
        RETOMADA("INTERROMPIDO", "EM_EXECUCAO"),
        CONCLUSAO("EM_EXECUCAO", "CONCLUIDO"),
        ENCERRAMENTO_SEM_CONCLUSAO("INTERROMPIDO", "ENCERRADO_SEM_CONCLUSAO");

        private final String estadoAnterior;
        private final String estadoResultante;

        Acao(String estadoAnterior, String estadoResultante) {
            this.estadoAnterior = estadoAnterior;
            this.estadoResultante = estadoResultante;
        }

        boolean usaCicloExecucao() {
            return this == OCORRENCIA || this == INTERRUPCAO;
        }

        public String estadoResultante() {
            return estadoResultante;
        }
    }

    public record Vinculos(boolean prestadorResponsavel, boolean motoristaDesignado) {
        public boolean podeConsultar() {
            return prestadorResponsavel || motoristaDesignado;
        }

        public boolean podeConfirmar(Acao acao) {
            return (acao == Acao.INICIO || acao == Acao.CONCLUSAO) ? motoristaDesignado : podeConsultar();
        }
    }

    public static Vinculos vinculos(long conta, long prestadorId, Long contaMotoristaAtual) {
        return new Vinculos(conta == prestadorId, Objects.equals(contaMotoristaAtual, conta));
    }

    public record Contexto(
            String estado, long revisaoDesignacao, Long cicloExecucaoId, Long interrupcaoId) {
        private Long referenciaPara(Acao acao) {
            return switch (acao) {
                case INICIO -> null;
                case OCORRENCIA, INTERRUPCAO, CONCLUSAO -> cicloExecucaoId;
                case RETOMADA, ENCERRAMENTO_SEM_CONCLUSAO -> interrupcaoId;
            };
        }
    }

    public static List<Acao> acoesElegiveis(Vinculos vinculos, Contexto vigente, boolean recursosValidos) {
        return Arrays.stream(Acao.values())
                .filter(acao -> novaConfirmacaoElegivel(acao, vinculos, vigente, vigente, recursosValidos))
                .toList();
    }

    public static boolean novaConfirmacaoElegivel(
            Acao acao, Vinculos vinculos, Contexto vigente, Contexto informado, boolean recursosValidos) {
        if (!vinculos.podeConfirmar(acao)
                || !acao.estadoAnterior.equals(vigente.estado())
                || !vigente.estado().equals(informado.estado())
                || vigente.revisaoDesignacao() != informado.revisaoDesignacao()) {
            return false;
        }
        if ((acao == Acao.INICIO || acao == Acao.RETOMADA) && !recursosValidos) {
            return false;
        }
        Long referencia = vigente.referenciaPara(acao);
        return acao == Acao.INICIO
                || (referencia != null && referencia.equals(informado.referenciaPara(acao)));
    }

    public static boolean dadosValidos(
            Acao acao, String texto, LocalDate data, LocalTime horario, boolean confirmou) {
        if (acao == Acao.INICIO) return true;
        if (data == null || horario == null || texto == null
                || ESPACOS.matcher(texto).matches()) {
            return false;
        }
        return (acao != Acao.RETOMADA && acao != Acao.ENCERRAMENTO_SEM_CONCLUSAO) || confirmou;
    }
}
