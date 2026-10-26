package com.aman.hospital_management.dto;

public record MyProfileResponse(Long userId, String name, String email, String profileImageUrl,
                                String specialization) {
}
