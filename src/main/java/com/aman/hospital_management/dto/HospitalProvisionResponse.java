package com.aman.hospital_management.dto;

public record HospitalProvisionResponse(
        Long hospitalId,
        String name,
        String code,
        Long adminId,
        String adminName,
        String adminEmail
) { }
