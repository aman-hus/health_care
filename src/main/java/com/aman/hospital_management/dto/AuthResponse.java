package com.aman.hospital_management.dto;

public class AuthResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
    private String role;
    private Long hospitalId;
    private String profileImageUrl;
    private String mrnNumber;


    public AuthResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role,
            Long hospitalId,
            String profileImageUrl,
            String mrnNumber
    ) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.hospitalId = hospitalId;
        this.profileImageUrl = profileImageUrl;
        this.mrnNumber = mrnNumber;
    }


    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getHospitalId() { return hospitalId; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getMrnNumber() { return mrnNumber; }
}
