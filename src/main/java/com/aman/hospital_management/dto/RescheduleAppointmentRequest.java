package com.aman.hospital_management.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(@NotNull LocalDateTime appointmentAt) { }
