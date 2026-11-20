package com.chubb.claims;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/workload")
class WorkloadController {
    private final ClaimRepository claims;

    WorkloadController(ClaimRepository claims) {
        this.claims = claims;
    }

    @GetMapping
    WorkloadSummary summary() {
        var totals = emptyCounts();
        var byOfficer = new TreeMap<String, Map<Claim.Status, Long>>();
        for (var row : claims.countByOfficerAndStatus()) {
            totals.merge(row.getStatus(), row.getClaimCount(), Long::sum);
            if (row.getOfficer() != null) {
                byOfficer.computeIfAbsent(row.getOfficer(), officer -> emptyCounts())
                        .put(row.getStatus(), row.getClaimCount());
            }
        }

        var officers = byOfficer.entrySet().stream()
                .map(entry -> new OfficerWorkload(entry.getKey(), openCount(entry.getValue()), entry.getValue()))
                .toList();
        return new WorkloadSummary(
                totals.values().stream().mapToLong(Long::longValue).sum(),
                openCount(totals),
                totals,
                officers);
    }

    private static Map<Claim.Status, Long> emptyCounts() {
        var counts = new EnumMap<Claim.Status, Long>(Claim.Status.class);
        for (var status : Claim.Status.values()) {
            counts.put(status, 0L);
        }
        return counts;
    }

    private static long openCount(Map<Claim.Status, Long> counts) {
        return counts.get(Claim.Status.SUBMITTED)
                + counts.get(Claim.Status.UNDER_REVIEW)
                + counts.get(Claim.Status.AWAITING_INFORMATION);
    }

    record WorkloadSummary(long totalClaims, long openClaims, Map<Claim.Status, Long> byStatus,
                           List<OfficerWorkload> byOfficer) {}

    record OfficerWorkload(String officer, long openClaims, Map<Claim.Status, Long> byStatus) {}
}
