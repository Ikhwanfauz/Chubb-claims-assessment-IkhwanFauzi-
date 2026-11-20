package com.chubb.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "claims")
public class Claim {
    public enum Type { MOTOR, PROPERTY }
    public enum Status { SUBMITTED, UNDER_REVIEW, AWAITING_INFORMATION, SETTLED, REJECTED }
    public enum Outcome { SETTLED, REJECTED }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, length = 2)
    private String market;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(length = 80)
    private String assignedOfficer;

    @Column(length = 1000)
    private String decisionReason;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    private UUID informationRequestId;

    @Column(length = 1000)
    private String informationQuestion;

    @Column(length = 2000)
    private String informationAnswer;

    @Column(precision = 15, scale = 3)
    private BigDecimal estimatedAmount;

    @Column(length = 3)
    private String estimateCurrency;

    protected Claim() {}

    Claim(Type type, String market, LocalDate incidentDate, String description) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.market = market;
        this.incidentDate = incidentDate;
        this.description = description.strip();
        this.status = Status.SUBMITTED;
        this.submittedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    void assign(String officer) {
        requireStatus(Status.SUBMITTED);
        this.assignedOfficer = officer.strip();
        this.status = Status.UNDER_REVIEW;
    }

    void requestInformation(String question) {
        requireStatus(Status.UNDER_REVIEW);
        informationRequestId = UUID.randomUUID();
        informationQuestion = question.strip();
        informationAnswer = null;
        status = Status.AWAITING_INFORMATION;
    }

    void provideInformation(UUID requestId, String answer) {
        requireStatus(Status.AWAITING_INFORMATION);
        if (!requestId.equals(informationRequestId)) {
            throw new InvalidClaimTransition("This information request is no longer current");
        }
        informationAnswer = answer.strip();
        status = Status.UNDER_REVIEW;
    }

    void updateEstimate(BigDecimal amount, String currency) {
        requireStatus(Status.UNDER_REVIEW);
        estimatedAmount = amount;
        estimateCurrency = currency;
    }

    void decide(Outcome outcome, String reason) {
        requireStatus(Status.UNDER_REVIEW);
        this.status = switch (outcome) {
            case SETTLED -> Status.SETTLED;
            case REJECTED -> Status.REJECTED;
        };
        this.decisionReason = reason.strip();
    }

    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new InvalidClaimTransition("Expected " + expected + " but claim is " + status);
        }
    }

    ClaimResponse toResponse() {
        var informationRequest = informationRequestId == null ? null
                : new ClaimResponse.InformationRequest(informationRequestId, informationQuestion, informationAnswer);
        return new ClaimResponse(id, type, market, incidentDate, description, status, submittedAt,
                assignedOfficer, decisionReason, informationRequest, estimatedAmount, estimateCurrency);
    }
}
