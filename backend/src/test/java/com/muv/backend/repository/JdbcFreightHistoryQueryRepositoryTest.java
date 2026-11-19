package com.muv.backend.repository;

import com.muv.backend.dto.FreightHistoryProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("rf001-local")
@Transactional
@Sql("/db/rf014-freight-history.sql")
class JdbcFreightHistoryQueryRepositoryTest {

    private static final YearMonth NOVEMBER_2026 = YearMonth.of(2026, 11);

    @Autowired
    private FreightHistoryQueryRepository repository;

    @Test
    void restrictsClientHistoryAndAppliesModalityStatusAndPeriodFilters() {
        var result = repository.findHistory(
                914001L,
                FreightHistoryProfile.CLIENTE,
                "ENCOMENDA",
                "PROPOSTA_ENVIADA",
                NOVEMBER_2026);

        assertThat(result)
                .extracting(FreightHistoryQueryRepository.HistoryRow::solicitationId)
                .containsExactly(914101L);
        assertThat(result.getFirst().requestSummary()).isEqualTo("12 caixas de teste");

        assertThat(repository.findHistory(
                914001L,
                FreightHistoryProfile.CLIENTE,
                null,
                null,
                YearMonth.of(2026, 10)))
                .isEmpty();
    }

    @Test
    void exposesOnlyRecordsAndProposalsAuthorizedForProvider() {
        var history = repository.findHistory(
                914002L,
                FreightHistoryProfile.PRESTADOR,
                null,
                null,
                NOVEMBER_2026);

        assertThat(history)
                .extracting(FreightHistoryQueryRepository.HistoryRow::solicitationId)
                .containsExactly(914102L, 914101L);

        var proposals = repository.findProposals(
                914101L,
                914002L,
                FreightHistoryProfile.PRESTADOR);

        assertThat(proposals)
                .extracting(FreightHistoryQueryRepository.ProposalRow::id)
                .containsExactly(914201L);
    }

    @Test
    void clientCanSeeAllProposalsFromOwnRequestButNotAnotherClientsHistory() {
        assertThat(repository.findProposals(
                914101L,
                914001L,
                FreightHistoryProfile.CLIENTE))
                .extracting(FreightHistoryQueryRepository.ProposalRow::id)
                .containsExactly(914202L, 914201L);

        assertThat(repository.findHistory(
                914004L,
                FreightHistoryProfile.CLIENTE,
                null,
                null,
                NOVEMBER_2026))
                .extracting(FreightHistoryQueryRepository.HistoryRow::solicitationId)
                .containsExactly(914103L);
    }

    @Test
    void loadsFreightTimelineInReverseChronologicalOrder() {
        assertThat(repository.findTimeline(914301L))
                .extracting(FreightHistoryQueryRepository.TimelineRow::eventType)
                .containsExactly("CONCLUSAO", "INICIO");
        assertThat(repository.findTimeline(null)).isEmpty();
    }
}
