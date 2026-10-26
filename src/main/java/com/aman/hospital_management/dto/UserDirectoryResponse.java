package com.aman.hospital_management.dto;

import com.aman.hospital_management.model.Role;

import java.time.LocalDateTime;

/** Public user-directory fields; credentials are deliberately excluded. */
public record UserDirectoryResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        String profileImageUrl,
        String specialization,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
