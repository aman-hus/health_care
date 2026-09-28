package com.aman.hospital_management.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PatientProfileResponse {
    private Long id;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
