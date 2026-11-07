package com.aman.hospital_management.dto;

import java.time.LocalDateTime;

public record DoctorTimeSlotResponse(LocalDateTime startAt, LocalDateTime endAt) { }
