package com.chubb.claims;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/claims")
class ClaimController {
    private final ClaimRepository claims;

    private final ClaimWorkflow workflow;

    ClaimController(ClaimRepository claims, ClaimWorkflow workflow) {
        this.claims = claims;
        this.workflow = workflow;
    }

    @PostMapping
    ResponseEntity<ClaimResponse> submit(@Valid @RequestBody SubmitClaim request) {
        var claim = new Claim(request.type(), request.market(), request.incidentDate(), request.description());
        var response = claims.save(claim).toResponse();
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    ClaimResponse find(@PathVariable UUID id) {
        return claims.findById(id)
                .map(Claim::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }

    @GetMapping
    List<ClaimResponse> list(@RequestParam(required = false) Claim.Status status) {
        var queue = status == null
                ? claims.findAllByOrderBySubmittedAtAscIdAsc()
                : claims.findByStatusOrderBySubmittedAtAscIdAsc(status);
        return queue.stream().map(Claim::toResponse).toList();
    }

    @PostMapping("/{id}/assignment")
    ClaimResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignClaim request) {
        return workflow.assign(id, request.officer());
    }

    @PostMapping("/{id}/decision")
    ClaimResponse decide(@PathVariable UUID id, @Valid @RequestBody DecideClaim request) {
        return workflow.decide(id, request.outcome(), request.reason());
    }

    @PostMapping("/{id}/information-requests")
    ClaimResponse requestInformation(@PathVariable UUID id, @Valid @RequestBody RequestInformation request) {
        return workflow.requestInformation(id, request.question());
    }

    @PostMapping("/{id}/information-requests/{requestId}/response")
    ClaimResponse provideInformation(@PathVariable UUID id, @PathVariable UUID requestId,
                                     @Valid @RequestBody ProvideInformation request) {
        return workflow.provideInformation(id, requestId, request.answer());
    }

    @ExceptionHandler(InvalidClaimTransition.class)
    ProblemDetail invalidTransition(InvalidClaimTransition error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }

    record AssignClaim(@NotBlank @Size(max = 80) String officer) {}

    record DecideClaim(
            @NotNull Claim.Outcome outcome,
            @NotBlank @Size(max = 1000) String reason
    ) {}

    record RequestInformation(@NotBlank @Size(max = 1000) String question) {}

    record ProvideInformation(@NotBlank @Size(max = 2000) String answer) {}

    record SubmitClaim(
            @NotNull Claim.Type type,
            @NotBlank @Pattern(regexp = "[A-Z]{2}") String market,
            @NotNull @PastOrPresent LocalDate incidentDate,
            @NotBlank @Size(max = 2000) String description
    ) {}
}
