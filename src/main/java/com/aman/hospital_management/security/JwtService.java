package com.aman.hospital_management.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long expiration;


    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiration = expiration;
    }


    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    public String generateToken(UserDetails userDetails, Long hospitalId) {

        Date now = new Date();

        Date expiryDate =
                new Date(now.getTime() + expiration);


        var builder = Jwts.builder()

                .subject(userDetails.getUsername())

                .issuedAt(now)

                .expiration(expiryDate)

                .signWith(secretKey);
        if (hospitalId != null) builder.claim("hospitalId", hospitalId);
        return builder.compact();
    }

    /** Parse the token only after verifying its signature and expiration. */
    public Claims extractClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
