package com.aman.hospital_management.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

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

        Date now = new Date();

        Date expiryDate =
                new Date(now.getTime() + expiration);


        return Jwts.builder()

                .subject(userDetails.getUsername())

                .issuedAt(now)

                .expiration(expiryDate)

                .signWith(secretKey)

                .compact();
    }
}