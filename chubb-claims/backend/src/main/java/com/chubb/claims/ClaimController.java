package com.chubb.claims;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/claims")
class ClaimController {
    private final ClaimRepository claims;

    ClaimController(ClaimRepository claims) {
        this.claims = claims;
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

    record SubmitClaim(
            @NotNull Claim.Type type,
            @NotBlank @Pattern(regexp = "[A-Z]{2}") String market,
            @NotNull @PastOrPresent LocalDate incidentDate,
            @NotBlank @Size(max = 2000) String description
    ) {}
}
