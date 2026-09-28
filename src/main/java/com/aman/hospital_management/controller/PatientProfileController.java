package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.PatientProfileRequest;
import com.aman.hospital_management.dto.PatientProfileResponse;
import com.aman.hospital_management.service.PatientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient/profile")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService profileService;

    @PostMapping
    public ResponseEntity<PatientProfileResponse> create(
            Authentication authentication,
            @Valid @RequestBody PatientProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.create(authentication.getName(), request));
    }

    @GetMapping
    public PatientProfileResponse get(Authentication authentication) {
        return profileService.get(authentication.getName());
    }

    @PutMapping
    public PatientProfileResponse update(
            Authentication authentication,
            @Valid @RequestBody PatientProfileRequest request) {
        return profileService.update(authentication.getName(), request);
    }
}
