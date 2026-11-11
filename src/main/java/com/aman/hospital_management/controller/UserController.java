package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.UserDirectoryResponse;
import com.aman.hospital_management.dto.MyProfileResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityRequest;
import com.aman.hospital_management.dto.DoctorDateAvailabilityResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityScheduleRequest;
import com.aman.hospital_management.dto.DoctorAvailabilityPageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PutMapping;
import com.aman.hospital_management.service.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserDirectoryService userDirectoryService;

    @GetMapping("/me")
    public MyProfileResponse getMyProfile(Authentication authentication) {
        return userDirectoryService.getMyProfile(authentication.getName());
    }

    @PatchMapping("/me/availability")
    @ResponseStatus(HttpStatus.OK)
    public DoctorDateAvailabilityResponse updateMyAvailability(Authentication authentication,
            @Valid @RequestBody DoctorAvailabilityRequest request) {
        return userDirectoryService.updateDoctorAvailability(authentication.getName(), request.date(), request.available(),
                request.workingStartTime(), request.workingEndTime());
    }

    @GetMapping("/me/availability")
    public DoctorAvailabilityPageResponse getMyAvailability(Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be 0 or greater and size must be between 1 and 100.");
        }
        return userDirectoryService.getMyAvailability(authentication.getName(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "date")));
    }

    @PutMapping("/me/availability/schedule")
    public java.util.Map<String, Object> setMyAvailabilitySchedule(Authentication authentication,
            @Valid @RequestBody DoctorAvailabilityScheduleRequest request) {
        return userDirectoryService.setDoctorAvailabilitySchedule(authentication.getName(), request);
    }

    @GetMapping
    public List<UserDirectoryResponse> getAllUsers() {
        return userDirectoryService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserDirectoryResponse getUserById(@PathVariable Long id) {
        return userDirectoryService.getUserById(id);
    }
}
