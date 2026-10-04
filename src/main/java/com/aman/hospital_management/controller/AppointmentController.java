package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.AppointmentRequest;
import com.aman.hospital_management.dto.AppointmentResponse;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(Authentication authentication, @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.book(authentication.getName(), currentRole(authentication), request);
    }

    @GetMapping("/mine")
    public List<AppointmentResponse> mine(Authentication authentication) {
        return appointmentService.getMine(authentication.getName(), currentRole(authentication));
    }

    private Role currentRole(Authentication authentication) {
        return Role.valueOf(authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .findFirst().orElseThrow());
    }
}
