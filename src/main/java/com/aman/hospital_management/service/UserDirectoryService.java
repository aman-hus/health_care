package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.UserDirectoryResponse;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.repository.UserRepository;
import com.aman.hospital_management.dto.MyProfileResponse;
import com.aman.hospital_management.security.HospitalContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDirectoryService {

    private final HospitalMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new MyProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getProfileImageUrl());
    }

    @Transactional(readOnly = true)
    public List<UserDirectoryResponse> getAllUsers() {
        return membershipRepository.findByHospital_IdAndEnabledTrueOrderByUser_NameAsc(HospitalContext.requireHospitalId()).stream()
                .map(membership -> {
                    var user = membership.getUser();
                    return new UserDirectoryResponse(user.getId(), user.getName(), user.getEmail(), membership.getRole(),
                            user.isEnabled(), user.getProfileImageUrl(), user.getCreatedAt(), user.getUpdatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDirectoryResponse getUserById(Long id) {
        var membership = membershipRepository.findByHospital_IdAndUser_IdAndEnabledTrue(HospitalContext.requireHospitalId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        var user = membership.getUser();

        return new UserDirectoryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                membership.getRole(),
                user.isEnabled(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}
