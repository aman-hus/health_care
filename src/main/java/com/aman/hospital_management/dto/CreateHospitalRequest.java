package com.aman.hospital_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHospitalRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 120) String adminName,
        @NotBlank @jakarta.validation.constraints.Email String adminEmail,
        @NotBlank @Size(min = 8, message = "Password must contain at least 8 characters") String adminPassword
) { }
