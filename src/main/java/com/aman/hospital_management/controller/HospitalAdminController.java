package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.CreateHospitalRequest;
import com.aman.hospital_management.dto.HospitalSummary;
import com.aman.hospital_management.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/hospitals")
@RequiredArgsConstructor
public class HospitalAdminController {
    private final HospitalService hospitalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public com.aman.hospital_management.dto.HospitalProvisionResponse create(@Valid @RequestBody CreateHospitalRequest request) {
        return hospitalService.create(request);
    }

    @GetMapping
    public List<HospitalSummary> list() { return hospitalService.list(); }
}
