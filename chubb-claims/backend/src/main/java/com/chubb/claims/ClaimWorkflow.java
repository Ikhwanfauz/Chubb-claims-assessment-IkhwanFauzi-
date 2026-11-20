package com.chubb.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class ClaimWorkflow {
    private final ClaimRepository claims;

    private final ApplicationEventPublisher events;

    ClaimWorkflow(ClaimRepository claims, ApplicationEventPublisher events) {
        this.claims = claims;
        this.events = events;
    }

    @Transactional
    public ClaimResponse assign(UUID id, String officer) {
        var claim = lockClaim(id);
        claim.assign(officer);
        return claim.toResponse();
    }

    @Transactional
    public ClaimResponse decide(UUID id, Claim.Outcome outcome, String reason) {
        var claim = lockClaim(id);
        claim.decide(outcome, reason);
        var response = claim.toResponse();
        events.publishEvent(new ClaimDecisionRecorded(id, response.status(), Instant.now()));
        return response;
    }

    @Transactional
    public ClaimResponse requestInformation(UUID id, String question) {
        var claim = lockClaim(id);
        claim.requestInformation(question);
        return claim.toResponse();
    }

    @Transactional
    public ClaimResponse provideInformation(UUID id, UUID requestId, String answer) {
        var claim = lockClaim(id);
        claim.provideInformation(requestId, answer);
        return claim.toResponse();
    }

    @Transactional
    public ClaimResponse updateEstimate(UUID id, BigDecimal amount, String code) {
        Currency currency;
        try {
            currency = Currency.getInstance(code);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code");
        }
        int fractionDigits = currency.getDefaultFractionDigits();
        if (fractionDigits < 0 || amount.stripTrailingZeros().scale() > fractionDigits) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount precision for currency");
        }

        var claim = lockClaim(id);
        claim.updateEstimate(amount.setScale(fractionDigits), code);
        return claim.toResponse();
    }

    private Claim lockClaim(UUID id) {
        return claims.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }
}
