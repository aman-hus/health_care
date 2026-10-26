package com.aman.hospital_management.service;

import com.aman.hospital_management.model.Hospital;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Builds a readable, globally unique MRN from the hospital name and database user ID. */
@Component
public class MrnNumberGenerator {

    public String generate(Hospital hospital, Long userId) {
        String letters = hospital.getName().replaceAll("[^A-Za-z]", "");
        String prefix = letters.substring(0, Math.min(3, letters.length())).toUpperCase(Locale.ROOT);
        return prefix + "-" + userId;
    }
}
