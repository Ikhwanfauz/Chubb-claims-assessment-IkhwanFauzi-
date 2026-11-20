package com.chubb.claims;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findAllByOrderBySubmittedAtAscIdAsc();

    List<Claim> findByStatusOrderBySubmittedAtAscIdAsc(Claim.Status status);

    @Query("""
            select c.assignedOfficer as officer, c.status as status, count(c) as claimCount
            from Claim c
            group by c.assignedOfficer, c.status
            """)
    List<WorkloadCount> countByOfficerAndStatus();

    interface WorkloadCount {
        String getOfficer();
        Claim.Status getStatus();
        long getClaimCount();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Claim c where c.id = ?1")
    Optional<Claim> findForUpdate(UUID id);
}
