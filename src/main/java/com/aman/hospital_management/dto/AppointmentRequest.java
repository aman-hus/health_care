package com.aman.hospital_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentRequest(
        Long patientId,
        @NotNull Long doctorId,
        Long nurseId,
        @NotNull LocalDateTime appointmentAt,
        @Size(max = 1000) String reason
) { }
