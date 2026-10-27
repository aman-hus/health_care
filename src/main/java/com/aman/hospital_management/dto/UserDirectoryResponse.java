package com.aman.hospital_management.dto;

import com.aman.hospital_management.model.Role;

import com.aman.hospital_management.model.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Public user-directory fields; credentials are deliberately excluded. */
public record UserDirectoryResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        String contactNumber,
        Gender gender,
        LocalDate dateOfBirth,
        String profileImageUrl,
        String specialization,
        String mrnNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
