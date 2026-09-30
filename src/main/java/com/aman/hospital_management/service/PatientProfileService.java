package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.PatientProfileRequest;
import com.aman.hospital_management.dto.PatientProfileResponse;
import com.aman.hospital_management.exception.UserNotFoundException;
import com.aman.hospital_management.model.AppUser;
import com.aman.hospital_management.model.PatientProfile;
import com.aman.hospital_management.repository.PatientProfileRepository;
import com.aman.hospital_management.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientProfileService {

    private final PatientProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional
    public PatientProfileResponse create(String email, PatientProfileRequest request) {
        AppUser user = findUser(email);
        if (profileRepository.existsByUserId(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient profile already exists");
        }

        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        apply(profile, request);
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public PatientProfileResponse get(String email, Long userId) {
        AppUser user = findUser(email);
        requireOwnProfile(user, userId);
        PatientProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient profile not found"));
        return toResponse(profile);
    }

    @Transactional
    public PatientProfileResponse update(String email, Long userId, PatientProfileRequest request) {
        AppUser user = findUser(email);
        requireOwnProfile(user, userId);
        PatientProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient profile not found"));
        apply(profile, request);
        return toResponse(profileRepository.save(profile));
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void requireOwnProfile(AppUser user, Long userId) {
        if (!user.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own profile");
        }
    }

    private void apply(PatientProfile profile, PatientProfileRequest request) {
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setAddress(request.getAddress());
        profile.setEmergencyContactName(request.getEmergencyContactName());
        profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
    }

    private PatientProfileResponse toResponse(PatientProfile profile) {
        return new PatientProfileResponse(profile.getId(), profile.getDateOfBirth(), profile.getPhoneNumber(),
                profile.getAddress(), profile.getEmergencyContactName(), profile.getEmergencyContactPhone(),
                profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
