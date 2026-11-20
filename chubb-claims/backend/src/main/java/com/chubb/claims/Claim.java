package com.chubb.claims;

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

@Entity
@Table(name = "claims")
public class Claim {
    public enum Type { MOTOR, PROPERTY }
    public enum Status { SUBMITTED }

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
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

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

    ClaimResponse toResponse() {
        return new ClaimResponse(id, type, market, incidentDate, description, status, submittedAt);
    }
}
