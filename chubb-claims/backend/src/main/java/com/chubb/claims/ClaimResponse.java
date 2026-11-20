package com.chubb.claims;

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
        Instant submittedAt
) {}
