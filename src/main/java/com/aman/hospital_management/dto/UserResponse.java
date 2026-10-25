package com.aman.hospital_management.dto;

import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.model.Gender;
import java.time.LocalDate;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private Long hospitalId;
    private String contactNumber;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String specialization;


    public UserResponse() {
    }


    public UserResponse(
            Long id,
            String name,
            String email,
            Role role,
            boolean enabled,
            Long hospitalId
    ) {
        this(id, name, email, role, enabled, hospitalId, null, null, null, null);
    }

    public UserResponse(Long id, String name, String email, Role role, boolean enabled,
                        Long hospitalId, String contactNumber, Gender gender, LocalDate dateOfBirth) {
        this(id, name, email, role, enabled, hospitalId, contactNumber, gender, dateOfBirth, null);
    }

    public UserResponse(Long id, String name, String email, Role role, boolean enabled,
                        Long hospitalId, String contactNumber, Gender gender, LocalDate dateOfBirth,
                        String specialization) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.hospitalId = hospitalId;
        this.contactNumber = contactNumber;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.specialization = specialization;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getHospitalId() { return hospitalId; }

    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}
