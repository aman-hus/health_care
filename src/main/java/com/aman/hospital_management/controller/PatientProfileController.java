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
@RequestMapping("/api/profile")
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

    @GetMapping("/{userId}")
    public PatientProfileResponse get(Authentication authentication, @PathVariable Long userId) {
        return profileService.get(authentication.getName(), userId);
    }



    @PutMapping("/{userId}")
    public PatientProfileResponse update(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody PatientProfileRequest request) {
        return profileService.update(authentication.getName(), userId, request);
    }
}
