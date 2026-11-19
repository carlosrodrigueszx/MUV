package com.muv.backend.service;

import com.muv.backend.dto.FreightHistoryProfile;
import com.muv.backend.dto.FreightHistoryResponse;
import com.muv.backend.repository.FreightHistoryQueryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class FreightHistoryService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM. yyyy", PT_BR);
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM.", PT_BR);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final FreightHistoryQueryRepository repository;

    public FreightHistoryService(FreightHistoryQueryRepository repository) {
        this.repository = repository;
    }

    public List<FreightHistoryProfile> availableProfiles(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A conta autenticada é obrigatória.");
        }
        return List.of(FreightHistoryProfile.values()).stream()
                .filter(profile -> repository.hasProfile(userId, profile))
                .toList();
    }

    public List<FreightHistoryResponse> list(
            Long userId,
            FreightHistoryProfile profile,
            String modality,
            String status,
            YearMonth month) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A conta autenticada é obrigatória.");
        }
        if (profile == null) {
            throw new IllegalArgumentException("O perfil de consulta é obrigatório.");
        }
        if (!repository.hasProfile(userId, profile)) {
            throw new AccessDeniedException("A conta não possui o perfil solicitado.");
        }

        String normalizedModality = normalizeModality(modality);
        String normalizedStatus = normalizeStatus(status);

        return repository.findHistory(userId, profile, normalizedModality, normalizedStatus, month)
                .stream()
                .map(row -> toResponse(row, userId, profile))
                .toList();
    }

    private FreightHistoryResponse toResponse(
            FreightHistoryQueryRepository.HistoryRow row,
            Long userId,
            FreightHistoryProfile profile) {
        var proposals = repository.findProposals(row.solicitationId(), userId, profile)
                .stream()
                .map(proposal -> new FreightHistoryResponse.ProposalDetails(
                        "P-" + proposal.id(),
                        proposal.provider(),
                        formatCurrency(proposal.amount()),
                        proposalStatusLabel(proposal.status())))
                .toList();

        var timeline = repository.findTimeline(row.freightId())
                .stream()
                .map(event -> new FreightHistoryResponse.TimelineEvent(
                        event.occurredAt().format(SHORT_DATE_FORMAT),
                        event.occurredAt().format(TIME_FORMAT),
                        eventLabel(event.eventType()),
                        eventDescription(event)))
                .toList();

        return new FreightHistoryResponse(
                row.freightId() == null ? "SL-" + row.solicitationId() : "FR-" + row.freightId(),
                modalityLabel(row.modality()),
                row.origin(),
                row.destination(),
                row.referenceDate().format(DATE_FORMAT),
                YearMonth.from(row.referenceDate()).toString(),
                row.agreedValue() == null ? "Ainda não acordado" : formatCurrency(row.agreedValue()),
                statusLabel(row.status()),
                new FreightHistoryResponse.RequestDetails(
                        row.openedAt().format(DATE_FORMAT),
                        row.requestedBy(),
                        row.requestSummary(),
                        row.currentConditions() == null ? "Condições ainda não acordadas." : row.currentConditions()),
                proposals,
                timeline);
    }

    private String normalizeModality(String modality) {
        if (modality == null || modality.isBlank() || "TODAS".equalsIgnoreCase(modality)) return null;
        return switch (modality.trim().toUpperCase(PT_BR)) {
            case "PESSOA", "PESSOAS" -> "PESSOA";
            case "ENCOMENDA", "ENCOMENDAS" -> "ENCOMENDA";
            default -> throw new IllegalArgumentException("Modalidade inválida.");
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "TODAS".equalsIgnoreCase(status)) return null;
        return switch (status.trim().toUpperCase(PT_BR).replace(' ', '_')) {
            case "CONTRATADO" -> "CONTRATADO";
            case "CONCLUÍDO", "CONCLUIDO" -> "CONCLUIDO";
            case "CANCELADO" -> "CANCELADO";
            case "ENCERRADO_SEM_CONCLUSÃO", "ENCERRADO_SEM_CONCLUSAO" -> "ENCERRADO_SEM_CONCLUSAO";
            case "PROPOSTA_ENVIADA" -> "PROPOSTA_ENVIADA";
            default -> throw new IllegalArgumentException("Situação inválida.");
        };
    }

    private String modalityLabel(String modality) {
        return "PESSOA".equals(modality) ? "Pessoas" : "Encomendas";
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "CONCLUIDO" -> "Concluído";
            case "CANCELADO" -> "Cancelado";
            case "ENCERRADO_SEM_CONCLUSAO" -> "Encerrado sem conclusão";
            case "AGUARDANDO_DESIGNACAO", "PRONTO_INICIO", "EM_EXECUCAO", "INTERROMPIDO" -> "Contratado";
            default -> "Proposta enviada";
        };
    }

    private String proposalStatusLabel(String status) {
        return "ACEITA".equals(status) ? "Aceita" : "Encerrada";
    }

    private String eventLabel(String eventType) {
        return switch (eventType) {
            case "INICIO" -> "Execução iniciada";
            case "OCORRENCIA" -> "Ocorrência registrada";
            case "INTERRUPCAO" -> "Interrupção registrada";
            case "RETOMADA" -> "Execução retomada";
            case "CANCELAMENTO" -> "Frete cancelado";
            case "CONCLUSAO" -> "Entrega concluída";
            case "ENCERRAMENTO_SEM_CONCLUSAO" -> "Encerrado sem conclusão";
            case "DESIGNACAO" -> "Motorista e veículo designados";
            default -> "Recursos do frete atualizados";
        };
    }

    private String eventDescription(FreightHistoryQueryRepository.TimelineRow event) {
        String details = event.description() != null ? event.description() : event.justification();
        return details == null || details.isBlank()
                ? "Por " + event.author() + "."
                : "Por " + event.author() + ". " + details;
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
