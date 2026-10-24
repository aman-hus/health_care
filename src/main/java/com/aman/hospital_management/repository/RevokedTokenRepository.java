package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    long deleteByExpiresAtBefore(Instant now);
}
