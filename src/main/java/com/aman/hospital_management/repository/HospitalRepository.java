package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Hospital> findAllByEnabledTrueOrderByNameAsc();
}
