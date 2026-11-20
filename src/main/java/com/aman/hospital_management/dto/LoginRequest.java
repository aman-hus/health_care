package com.aman.hospital_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public class LoginRequest {
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String contactNumber;

    @NotBlank(message = "Password is required")
    private String password;
    private Long hospitalId;


    public LoginRequest() {
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
}
