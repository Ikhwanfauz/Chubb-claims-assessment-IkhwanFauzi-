package com.chubb.claims;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClaimRepository extends JpaRepository<Claim, UUID> {}
