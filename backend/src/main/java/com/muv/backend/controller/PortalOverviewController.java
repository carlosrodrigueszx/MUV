package com.muv.backend.controller;

import com.muv.backend.repository.AuthRepository;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PortalOverviewController {
    private static final Set<String> ACTIVE = Set.of(
            "AGUARDANDO_DESIGNACAO", "PRONTO_INICIO", "EM_EXECUCAO", "INTERROMPIDO");

    private static final Set<String> ACOMPANHAVEIS = Set.of("PRONTO_INICIO", "EM_EXECUCAO", "INTERROMPIDO");

    private final AuthRepository accounts;
    private final JdbcClient db;
    private final Clock clock;

    public PortalOverviewController(AuthRepository accounts, JdbcClient db, Clock clock) {
        this.accounts = accounts;
        this.db = db;
        this.clock = clock;
    }

    @GetMapping("/api/auth/visao-geral")
    public Overview overview(@RequestParam Portal portal, Principal principal) {
        long id = accounts.findAccount(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).id();
        var profiles = accounts.profiles(id);
        boolean autonomous = profiles.prestador() && !profiles.empresa();
        boolean companyPortal = portal == Portal.PRESTADOR;
        if (companyPortal ? !profiles.empresa() : !(profiles.motorista() || autonomous)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        boolean provider = companyPortal || autonomous;

        var now = LocalDateTime.now(clock);
        var month = YearMonth.from(now);
        var firstMonth = month.minusMonths(5);
        var trips = db.sql("""
                SELECT f.status, f.data_hora_conclusao, s.data_hora_pretendida,
                       CASE WHEN m.usuario_id = :id THEN f.id END AS frete_designado_id,
                       o.cidade AS origem, d.cidade AS destino,
                       CASE WHEN :provider THEN f.valor_atual ELSE NULL END AS valor
                FROM frete f
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                JOIN endereco o ON o.id = s.endereco_origem_id
                JOIN endereco d ON d.id = s.endereco_destino_id
                LEFT JOIN motorista m ON m.id = f.motorista_atual_id
                WHERE (CASE WHEN :provider THEN f.prestador_responsavel_id = :id ELSE m.usuario_id = :id END)
                  AND (f.status IN ('AGUARDANDO_DESIGNACAO','PRONTO_INICIO','EM_EXECUCAO','INTERROMPIDO')
                       OR (f.status = 'CONCLUIDO' AND f.data_hora_conclusao >= :start))
                ORDER BY s.data_hora_pretendida, f.id
                """).param("provider", provider).param("id", id)
                .param("start", firstMonth.atDay(1).atStartOfDay())
                .query((row, index) -> new Trip(row.getObject("frete_designado_id", Long.class), row.getString("status"),
                        row.getObject("data_hora_conclusao", LocalDateTime.class),
                        row.getObject("data_hora_pretendida", LocalDateTime.class),
                        row.getString("origem"), row.getString("destino"), row.getBigDecimal("valor")))
                .list();

        var completed = trips.stream().filter(trip -> trip.completedAt() != null
                && month.equals(YearMonth.from(trip.completedAt()))).toList();
        BigDecimal amount = provider ? completed.stream().map(Trip::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : null;
        BigDecimal rating = provider ? db.sql("""
                SELECT avaliacao_media FROM prestador WHERE id = :id AND total_avaliacoes > 0
                """).param("id", id).query(BigDecimal.class).optional().orElse(null) : null;
        var chart = IntStream.range(0, 6).mapToObj(firstMonth::plusMonths).map(period ->
                new Month(period.toString(), trips.stream().filter(trip -> trip.completedAt() != null
                        && period.equals(YearMonth.from(trip.completedAt()))).count())).toList();
        var upcoming = trips.stream().filter(trip -> ACTIVE.contains(trip.status()) && !trip.departure().isBefore(now))
                .limit(4).map(trip -> new AgendaItem(trip.departure(), trip.origin(), trip.destination())).toList();

        List<FreteAcompanhavel> fretesAcompanhaveis = companyPortal ? List.of() : trips.stream()
                .filter(trip -> trip.freteDesignadoId() != null && ACOMPANHAVEIS.contains(trip.status()))
                .map(trip -> new FreteAcompanhavel(trip.freteDesignadoId(), trip.origin(), trip.destination()))
                .toList();

        return new Overview(completed.size(), trips.stream().filter(trip -> ACTIVE.contains(trip.status())).count(),
                amount, rating, chart, upcoming, fretesAcompanhaveis);
    }

    public enum Portal { PRESTADOR, MOTORISTA }
    public record Overview(long completed, long active, BigDecimal amount, BigDecimal rating,
                           List<Month> chart, List<AgendaItem> upcoming, List<FreteAcompanhavel> fretesAcompanhaveis) {}
    public record FreteAcompanhavel(long freteId, String origin, String destination) {}
    public record Month(String month, long completed) {}
    public record AgendaItem(LocalDateTime departure, String origin, String destination) {}
    private record Trip(Long freteDesignadoId, String status, LocalDateTime completedAt, LocalDateTime departure,
                        String origin, String destination, BigDecimal amount) {}
}
