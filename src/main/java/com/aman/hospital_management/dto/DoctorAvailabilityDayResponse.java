package com.aman.hospital_management.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorAvailabilityDayResponse(
        LocalDate date,
        boolean available,
        LocalTime workingStartTime,
        LocalTime workingEndTime
) { }
