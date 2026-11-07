package com.aman.hospital_management.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorAvailabilityRequest(@NotNull LocalDate date, @NotNull Boolean available,
                                       LocalTime workingStartTime, LocalTime workingEndTime) { }
