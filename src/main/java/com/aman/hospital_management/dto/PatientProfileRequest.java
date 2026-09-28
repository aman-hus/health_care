package com.aman.hospital_management.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PatientProfileRequest {

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phoneNumber;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Size(max = 120, message = "Emergency contact name must be at most 120 characters")
    private String emergencyContactName;

    @Size(max = 30, message = "Emergency contact phone must be at most 30 characters")
    private String emergencyContactPhone;

}
