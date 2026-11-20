package com.chubb.claims;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/exposure")
class ExposureController {
    private final ClaimRepository claims;

    ExposureController(ClaimRepository claims) {
        this.claims = claims;
    }

    @GetMapping
    ExposureSummary summary() {
        var openStatuses = EnumSet.of(
                Claim.Status.SUBMITTED, Claim.Status.UNDER_REVIEW, Claim.Status.AWAITING_INFORMATION);
        var totals = new TreeMap<String, CurrencyExposure>();
        long missingEstimate = 0;

        for (var row : claims.sumExposureByCurrency(openStatuses)) {
            if (row.getCurrency() == null) {
                missingEstimate = row.getClaimCount();
            } else {
                totals.put(row.getCurrency(), new CurrencyExposure(row.getAmount(), row.getClaimCount()));
            }
        }

        return new ExposureSummary(missingEstimate, totals);
    }

    record ExposureSummary(long openClaimsWithoutEstimate, Map<String, CurrencyExposure> byCurrency) {}

    record CurrencyExposure(BigDecimal estimatedAmount, long claimCount) {}
}
