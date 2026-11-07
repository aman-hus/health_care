package com.aman.hospital_management.dto;

import java.time.LocalDate;

public record DoctorDateAvailabilityResponse(Long doctorId, LocalDate date, boolean available, String message) { }
