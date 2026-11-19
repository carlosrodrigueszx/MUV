package com.muv.backend.repository;

import com.muv.backend.dto.FreightHistoryProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public interface FreightHistoryQueryRepository {

    boolean hasProfile(Long userId, FreightHistoryProfile profile);

    List<HistoryRow> findHistory(
            Long userId,
            FreightHistoryProfile profile,
            String modality,
            String status,
            YearMonth month);

    List<ProposalRow> findProposals(
            Long solicitationId,
            Long userId,
            FreightHistoryProfile profile);

    List<TimelineRow> findTimeline(Long freightId);

    record HistoryRow(
            Long solicitationId,
            Long freightId,
            String modality,
            String origin,
            String destination,
            LocalDateTime referenceDate,
            LocalDateTime openedAt,
            String requestedBy,
            String requestSummary,
            BigDecimal agreedValue,
            String currentConditions,
            String status) {
    }

    record ProposalRow(
            Long id,
            String provider,
            BigDecimal amount,
            String status) {
    }

    record TimelineRow(
            LocalDateTime occurredAt,
            String eventType,
            String author,
            String description,
            String justification) {
    }
}
