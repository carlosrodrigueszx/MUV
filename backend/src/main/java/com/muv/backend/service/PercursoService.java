package com.muv.backend.service;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import java.net.URI;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

@Service
public class PercursoService {
    public record Coordenada(double longitude, double latitude) {}
    public record Ponto(String endereco, LocalDateTime horario, Coordenada coordenada) {}
    public record Percurso(List<Ponto> pontos, List<Coordenada> geometria, List<Long> segundosPorTrecho, BigDecimal distanciaKm) {}
    private record Geometry(List<Double> coordinates) {}
    private record Properties(String countrycode, String type) {}
    private record Feature(Geometry geometry, Properties properties) {}
    private record Busca(List<Feature> features) {}
    private record Linha(List<List<Double>> coordinates) {}
    private record Trecho(Double duration) {}
    private record Rota(Linha geometry, List<Trecho> legs, BigDecimal distance) {}
    private record Rotas(List<Rota> routes) {}

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper json;
    private final String photon;
    private final String osrm;
    private final Map<String, Coordenada> cache = Collections.synchronizedMap(new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Coordenada> eldest) {
            return size() > 512;
        }
    });

    public PercursoService(ObjectMapper json,
            @Value("${muv.percurso.photon:https://photon.komoot.io/api/}") String photon,
            @Value("${muv.percurso.osrm:https://router.project-osrm.org}") String osrm) {
        this.json = json;
        this.photon = photon;
        this.osrm = osrm;
    }

    public Percurso consultar(AvaliacaoSolicitacao avaliacao) {
        var pontos = new ArrayList<Ponto>();
        pontos.add(localizar(avaliacao.origem(), avaliacao.partida()));
        for (var parada : avaliacao.paradas()) pontos.add(localizar(parada.endereco(), parada.horario()));
        pontos.add(localizar(avaliacao.destino(), null));
        return consultar(pontos);
    }

    public BigDecimal distanciaKm(List<String> enderecos) {
        return consultar(enderecos.stream().map(endereco -> localizar(endereco, null)).toList()).distanciaKm();
    }

    private Percurso consultar(List<Ponto> pontos) {
        var indisponivel = new Percurso(pontos, List.of(), List.of(), null);
        if (pontos.stream().anyMatch(p -> p.coordenada() == null)) return indisponivel;
        String locais = pontos.stream().map(Ponto::coordenada)
                .map(c -> c.longitude() + "," + c.latitude()).collect(Collectors.joining(";"));
        var resposta = consultar(URI.create(osrm + "/route/v1/driving/" + locais
                + "?geometries=geojson&overview=full&steps=false"), Rotas.class);
        if (resposta == null || resposta.routes() == null || resposta.routes().isEmpty()) return indisponivel;
        var rota = resposta.routes().getFirst();
        if (rota.geometry() == null || rota.geometry().coordinates() == null || rota.legs() == null
                || rota.legs().size() != pontos.size() - 1) return indisponivel;
        var linha = rota.geometry().coordinates().stream().map(PercursoService::coordenada).toList();
        if (linha.size() < 2 || linha.contains(null)) return indisponivel;
        var segundos = rota.legs().stream().map(t -> t.duration() != null && t.duration() >= 0
                ? Math.round(t.duration()) : null).toList();
        return new Percurso(comHorarios(pontos, segundos), linha, segundos,
                rota.distance() == null ? null : rota.distance().movePointLeft(3));
    }

    static List<Ponto> comHorarios(List<Ponto> pontos, List<Long> segundos) {
        var previstos = new ArrayList<Ponto>();
        LocalDateTime anterior = null;
        for (int i = 0; i < pontos.size(); i++) {
            var ponto = pontos.get(i);
            var horario = ponto.horario();
            if (horario == null && anterior != null && segundos.get(i - 1) != null) {
                horario = anterior.plusSeconds(segundos.get(i - 1));
            }
            previstos.add(new Ponto(ponto.endereco(), horario, ponto.coordenada()));
            anterior = horario;
        }
        return previstos;
    }

    private Ponto localizar(String endereco, LocalDateTime horario) {
        var local = cache.get(endereco);
        if (local == null && !endereco.isBlank()) {
            var uri = UriComponentsBuilder.fromUriString(photon).queryParam("q", "{endereco}")
                    .queryParam("limit", 1).encode().buildAndExpand(endereco + ", Brasil").toUri();
            var busca = consultar(uri, Busca.class);
            if (busca != null && busca.features() != null && !busca.features().isEmpty()) {
                var item = busca.features().getFirst();
                if (item.geometry() != null && item.properties() != null
                        && "BR".equalsIgnoreCase(item.properties().countrycode())
                        && ("house".equals(item.properties().type()) || "street".equals(item.properties().type()))) {
                    local = coordenada(item.geometry().coordinates());
                    if (local != null) cache.put(endereco, local);
                }
            }
        }
        return new Ponto(endereco, horario, local);
    }

    private static Coordenada coordenada(List<Double> par) {
        if (par == null || par.size() < 2 || par.get(0) == null || par.get(1) == null
                || !(Math.abs(par.get(0)) <= 180 && Math.abs(par.get(1)) <= 90)) return null;
        return new Coordenada(par.get(0), par.get(1));
    }

    private <T> T consultar(URI uri, Class<T> tipo) {
        var requisicao = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4))
                .header("User-Agent", "MUV-RF007/1.0").GET().build();
        try {
            var resposta = client.send(requisicao, HttpResponse.BodyHandlers.ofString());
            return resposta.statusCode() == 200 ? json.readValue(resposta.body(), tipo) : null;
        } catch (IOException | tools.jackson.core.JacksonException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
