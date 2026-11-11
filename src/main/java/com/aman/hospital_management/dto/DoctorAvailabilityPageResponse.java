package com.aman.hospital_management.dto;

import java.util.List;

public record DoctorAvailabilityPageResponse(
        Long doctorId,
        List<DoctorAvailabilityDayResponse> dates,
        int page,
        int size,
        long totalElements,
        int totalPages
) { }
