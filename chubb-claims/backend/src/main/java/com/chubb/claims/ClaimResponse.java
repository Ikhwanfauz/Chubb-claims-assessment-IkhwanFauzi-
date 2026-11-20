package com.chubb.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        Claim.Type type,
        String market,
        LocalDate incidentDate,
        String description,
        Claim.Status status,
        Instant submittedAt,
        String assignedOfficer,
        String decisionReason,
        InformationRequest informationRequest,
        BigDecimal estimatedAmount,
        String estimateCurrency
) {
    public record InformationRequest(UUID id, String question, String answer) {}
}
