package com.chubb.claims;

import java.time.Instant;
import java.util.UUID;

record ClaimDecisionRecorded(UUID claimId, Claim.Status status, Instant decidedAt) {}
