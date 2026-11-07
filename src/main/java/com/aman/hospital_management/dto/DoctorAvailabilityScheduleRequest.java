package com.aman.hospital_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record DoctorAvailabilityScheduleRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LocalTime workingStartTime,
        @NotNull LocalTime workingEndTime,
        @NotNull @NotEmpty Set<@NotNull DayOfWeek> availableDays,
        Set<@NotNull LocalDate> availableDates,
        Set<@NotNull LocalDate> unavailableDates
) { }
