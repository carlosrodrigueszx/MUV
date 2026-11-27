package com.muv.backend.service;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class EstimativaFreteService {
    private static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");

    public record Estimativa(BigDecimal distanciaKm, BigDecimal consumoKmLitro, BigDecimal precoLitro,
            String fonteCombustivel, LocalDate dataCombustivel, BigDecimal combustivel,
            BigDecimal custosOperacionais, BigDecimal remuneracao, BigDecimal valorEstimado,
            BigDecimal piso, BigDecimal limiarAlerta, String motivo) {}

    public record LimiteProposta(boolean ativo, BigDecimal teto, String versao, String motivo) {}

    boolean tetoAtivo(String modalidade) {
        return configuracao.getProperty(prefixo(modalidade) + "teto-ativo", Boolean.class, false);
    }

    public LimiteProposta limiteProposta(AvaliacaoSolicitacao solicitacao, Estimativa estimativa) {
        String modalidade = solicitacao.modalidade();
        if (!tetoAtivo(modalidade)) return new LimiteProposta(false, null, null, null);
        String prefixo = prefixo(modalidade);
        var alerta = decimal(prefixo + "alerta-percentual");
        var percentual = configuracao.getProperty(prefixo + "teto-percentual", BigDecimal.class, new BigDecimal("50"));
        if (estimativa.valorEstimado() == null) {
            return new LimiteProposta(true, null, null, estimativa.motivo());
        }
        if (alerta == null || percentual.compareTo(alerta) < 0) {
            return new LimiteProposta(true, null, null, "O teto configurado está abaixo do limiar de alerta.");
        }
        var teto = estimativa.valorEstimado().multiply(BigDecimal.ONE.add(percentual.movePointLeft(2)))
                .setScale(2, RoundingMode.DOWN);
        String referencia = solicitacao.id() + "|" + modalidade + "|" + solicitacao.origem() + "|"
                + solicitacao.paradas() + "|" + solicitacao.destino() + "|" + solicitacao.partida()
                + "|" + solicitacao.retornoPrevisto() + "|" + estimativa + "|" + teto;
        String versao = UUID.nameUUIDFromBytes(referencia.getBytes(StandardCharsets.UTF_8)).toString();
        return new LimiteProposta(true, teto, versao, null);
    }

    private record Perfil(BigDecimal consumo, BigDecimal preco, String fonte, LocalDate data,
            BigDecimal operacaoKm, BigDecimal remuneracao, BigDecimal pisoFator, BigDecimal pisoMinimo,
            BigDecimal alertaPercentual) {}

    private final Environment configuracao;
    private final PercursoService percurso;

    public EstimativaFreteService(Environment configuracao, PercursoService percurso) {
        this.configuracao = configuracao;
        this.percurso = percurso;
    }

    public Estimativa calcularParaEnderecos(String modalidade, List<String> enderecos) {
        var perfil = perfil(modalidade);
        return perfil == null ? indisponivel("Referência de custos indisponível.")
                : calcular(perfil, percurso.distanciaKm(enderecos));
    }

    public Estimativa calcular(String modalidade, BigDecimal distanciaKm) {
        var perfil = perfil(modalidade);
        return perfil == null ? indisponivel("Referência de custos indisponível.") : calcular(perfil, distanciaKm);
    }

    private Estimativa calcular(Perfil perfil, BigDecimal distanciaKm) {
        if (distanciaKm == null || distanciaKm.signum() <= 0
                || distanciaKm.compareTo(new BigDecimal("99999999.99")) > 0) {
            return indisponivel("Distância viária indisponível.");
        }
        var combustivel = distanciaKm.multiply(perfil.preco()).divide(perfil.consumo(), 2, RoundingMode.HALF_UP);
        var operacao = moeda(distanciaKm.multiply(perfil.operacaoKm()));
        var custo = combustivel.add(operacao);
        var estimado = custo.add(perfil.remuneracao());
        if (estimado.signum() <= 0) return indisponivel("Custo de referência insuficiente para o percurso.");
        var piso = perfil.pisoFator() == null || operacao.signum() == 0 ? null
                : custo.multiply(perfil.pisoFator()).max(perfil.pisoMinimo()).setScale(2, RoundingMode.CEILING);
        if (piso != null && piso.compareTo(estimado) > 0) {
            return indisponivel("O piso configurado excede o valor estimado para este percurso.");
        }
        var alerta = estimado.multiply(BigDecimal.ONE.add(perfil.alertaPercentual().movePointLeft(2)))
                .setScale(2, RoundingMode.DOWN);
        return new Estimativa(distanciaKm, perfil.consumo(), perfil.preco(), perfil.fonte(), perfil.data(),
                combustivel, operacao, perfil.remuneracao(), estimado, piso, alerta, null);
    }

    private Perfil perfil(String modalidade) {
        String prefixo = prefixo(modalidade);
        var consumo = decimal(prefixo + "consumo-km-litro");
        var preco = decimal(prefixo + "preco-litro");
        var fonte = configuracao.getProperty(prefixo + "fonte-combustivel");
        var data = configuracao.getProperty(prefixo + "data-combustivel");
        var validade = configuracao.getProperty(prefixo + "validade-dias", Integer.class);
        var operacao = decimal(prefixo + "operacao-por-km");
        var remuneracao = decimal(prefixo + "remuneracao");
        var alerta = decimal(prefixo + "alerta-percentual");
        if (consumo == null || consumo.signum() <= 0 || preco == null || preco.signum() <= 0
                || fonte == null || fonte.isBlank() || data == null || validade == null || validade < 0
                || operacao == null || operacao.signum() < 0 || remuneracao == null || remuneracao.signum() < 0
                || alerta == null || alerta.signum() < 0) return null;
        LocalDate dataReferencia;
        try {
            dataReferencia = LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            return null;
        }
        var hoje = LocalDate.now(FORTALEZA);
        if (hoje.isBefore(dataReferencia) || hoje.isAfter(dataReferencia.plusDays(validade))) return null;
        var fator = decimal(prefixo + "piso-fator");
        var minimo = decimal(prefixo + "piso-minimo");
        if (fator == null || fator.signum() <= 0 || fator.compareTo(BigDecimal.ONE) > 0
                || minimo == null || minimo.signum() < 0) fator = null;
        return new Perfil(consumo, preco, fonte, dataReferencia, operacao, moeda(remuneracao), fator, minimo, alerta);
    }

    private static String prefixo(String modalidade) {
        return "muv.estimativa." + modalidade.toLowerCase(Locale.ROOT) + ".";
    }

    private BigDecimal decimal(String chave) {
        return configuracao.getProperty(chave, BigDecimal.class);
    }

    private static BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static Estimativa indisponivel(String motivo) {
        return new Estimativa(null, null, null, null, null, null, null, null, null, null, null, motivo);
    }
}
