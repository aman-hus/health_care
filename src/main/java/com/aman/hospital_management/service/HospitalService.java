package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.CreateHospitalRequest;
import com.aman.hospital_management.dto.HospitalProvisionResponse;
import com.aman.hospital_management.dto.HospitalSummary;
import com.aman.hospital_management.model.Hospital;
import com.aman.hospital_management.model.HospitalMembership;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.repository.HospitalRepository;
import com.aman.hospital_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MrnNumberGenerator mrnNumberGenerator;

    @Transactional
    public HospitalProvisionResponse create(CreateHospitalRequest request) {
        boolean platformAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
        if (!platformAdmin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only platform admins can create hospitals.");
        if (hospitalRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hospital code already exists.");
        }
        Hospital hospital = hospitalRepository.save(new Hospital(request.name().trim(), request.code().trim()));
        if (userRepository.existsByEmail(request.adminEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with the hospital admin email already exists.");
        }
        var hospitalAdmin = userRepository.save(new com.aman.hospital_management.model.AppUser(
                request.adminName().trim(),
                request.adminEmail().trim(),
                passwordEncoder.encode(request.adminPassword()),
                com.aman.hospital_management.model.Role.ADMIN));
        hospitalAdmin.setMrnNumber(mrnNumberGenerator.generate(hospital, hospitalAdmin.getId()));
        hospitalAdmin = userRepository.save(hospitalAdmin);
        membershipRepository.save(new HospitalMembership(hospital, hospitalAdmin, com.aman.hospital_management.model.Role.ADMIN));

        return new HospitalProvisionResponse(hospital.getId(), hospital.getName(), hospital.getCode(),
                hospitalAdmin.getId(), hospitalAdmin.getName(), hospitalAdmin.getEmail(), hospitalAdmin.getMrnNumber());
    }

    @Transactional(readOnly = true)
    public List<HospitalSummary> list() {
        return hospitalRepository.findAllByEnabledTrueOrderByNameAsc().stream()
                .map(h -> new HospitalSummary(h.getId(), h.getName(), h.getCode())).toList();
    }
}
