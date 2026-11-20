package com.chubb.claims;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class ClaimWorkflow {
    private final ClaimRepository claims;

    ClaimWorkflow(ClaimRepository claims) {
        this.claims = claims;
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
        return claim.toResponse();
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

    private Claim lockClaim(UUID id) {
        return claims.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }
}
