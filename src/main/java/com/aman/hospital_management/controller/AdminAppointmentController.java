package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.AppointmentResponse;
import com.aman.hospital_management.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public List<AppointmentResponse> all() { return appointmentService.getAll(); }

    @GetMapping("/patient/{patientId}")
    public List<AppointmentResponse> forPatient(@PathVariable Long patientId) {
        return appointmentService.getForPatient(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<AppointmentResponse> forDoctor(@PathVariable Long doctorId) {
        return appointmentService.getForDoctor(doctorId);
    }
}
