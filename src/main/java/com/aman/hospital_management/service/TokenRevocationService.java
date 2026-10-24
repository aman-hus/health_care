package com.aman.hospital_management.service;

import com.aman.hospital_management.model.RevokedToken;
import com.aman.hospital_management.repository.RevokedTokenRepository;
import com.aman.hospital_management.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revoke(String token) {
        Claims claims = jwtService.extractClaims(token);
        String hash = hash(token);
        Instant expiresAt = claims.getExpiration().toInstant();
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (!revokedTokenRepository.existsById(hash)) {
            revokedTokenRepository.save(new RevokedToken(hash, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hash(token), Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
