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
import java.util.Map;

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

    @DeleteMapping("/{appointmentId}")
    public Map<String, String> delete(Authentication authentication, @PathVariable Long appointmentId) {
        appointmentService.delete(authentication.getName(), currentRole(authentication), appointmentId);
        return Map.of("message", "Appointment deleted successfully.");
    }

    private Role currentRole(Authentication authentication) {
        return Role.valueOf(authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .findFirst().orElseThrow());
    }
}
