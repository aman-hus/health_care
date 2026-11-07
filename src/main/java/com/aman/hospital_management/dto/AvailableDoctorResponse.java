package com.aman.hospital_management.dto;

import java.time.LocalDate;

public record AvailableDoctorResponse(Long doctorId, String name, String specialization, LocalDate date) { }
