package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.AppointmentRequest;
import com.aman.hospital_management.dto.AppointmentResponse;
import com.aman.hospital_management.dto.RescheduleAppointmentRequest;
import com.aman.hospital_management.dto.DoctorDateAvailabilityResponse;
import com.aman.hospital_management.dto.AvailableDoctorResponse;
import com.aman.hospital_management.dto.DoctorTimeSlotResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityPageResponse;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    @GetMapping("/availability")
    public DoctorDateAvailabilityResponse availability(@RequestParam Long doctorId,
                                                        @RequestParam LocalDate date) {
        return appointmentService.checkAvailability(doctorId, date);
    }

    @GetMapping("/availability/doctors")
    public List<AvailableDoctorResponse> availableDoctors(@RequestParam LocalDate date) {
        return appointmentService.findAvailableDoctors(date);
    }

    @GetMapping("/availability/slots")
    public List<DoctorTimeSlotResponse> availableSlots(@RequestParam Long doctorId, @RequestParam LocalDate date) {
        return appointmentService.getAvailableSlots(doctorId, date);
    }

    @GetMapping("/availability/calendar")
    public DoctorAvailabilityPageResponse availabilityCalendar(@RequestParam Long doctorId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be 0 or greater and size must be between 1 and 100.");
        }
        return appointmentService.getAvailabilityCalendar(doctorId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "date")));
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

    @PostMapping("/{appointmentId}/accept")
    public AppointmentResponse accept(Authentication authentication, @PathVariable Long appointmentId) {
        return appointmentService.accept(authentication.getName(), currentRole(authentication), appointmentId);
    }

    @PostMapping("/{appointmentId}/reject")
    public AppointmentResponse reject(Authentication authentication, @PathVariable Long appointmentId) {
        return appointmentService.reject(authentication.getName(), currentRole(authentication), appointmentId);
    }

    @PatchMapping("/{appointmentId}/reschedule")
    public AppointmentResponse reschedule(Authentication authentication, @PathVariable Long appointmentId,
                                          @Valid @RequestBody RescheduleAppointmentRequest request) {
        return appointmentService.reschedule(authentication.getName(), currentRole(authentication),
                appointmentId, request.appointmentAt());
    }

    private Role currentRole(Authentication authentication) {
        return Role.valueOf(authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .findFirst().orElseThrow());
    }
}
