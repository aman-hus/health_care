package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.AuthResponse;
import com.aman.hospital_management.dto.LoginRequest;
import com.aman.hospital_management.dto.BootstrapAdminRequest;
import com.aman.hospital_management.dto.UserResponse;
import com.aman.hospital_management.dto.HospitalSummary;
import com.aman.hospital_management.dto.LogoutResponse;
import com.aman.hospital_management.service.HospitalService;
import com.aman.hospital_management.service.AuthService;
import com.aman.hospital_management.service.TokenRevocationService;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final HospitalService hospitalService;
    private final TokenRevocationService tokenRevocationService;


    public AuthController(AuthService authService, HospitalService hospitalService,
                          TokenRevocationService tokenRevocationService) {
        this.authService = authService;
        this.hospitalService = hospitalService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @GetMapping("/hospitals")
    public java.util.List<HospitalSummary> hospitals() { return hospitalService.list(); }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(Authentication authentication,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authentication == null || authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LogoutResponse(false, "A valid bearer token is required to log out."));
        }
        tokenRevocationService.revoke(authorization.substring(7).trim());
        return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully."));
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<UserResponse> bootstrapAdmin(
            @Valid @RequestBody BootstrapAdminRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.bootstrapAdmin(request));
    }
}
