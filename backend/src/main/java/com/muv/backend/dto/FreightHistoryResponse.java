package com.muv.backend.dto;

import java.util.List;

public record FreightHistoryResponse(
        String id,
        String modality,
        String origin,
        String destination,
        String displayDate,
        String month,
        String agreedValue,
        String status,
        RequestDetails request,
        List<ProposalDetails> proposals,
        List<TimelineEvent> timeline) {

    public record RequestDetails(
            String openedAt,
            String requestedBy,
            String summary,
            String preservedConditions) {
    }

    public record ProposalDetails(
            String id,
            String provider,
            String amount,
            String status) {
    }

    public record TimelineEvent(
            String date,
            String time,
            String title,
            String description) {
    }
}
