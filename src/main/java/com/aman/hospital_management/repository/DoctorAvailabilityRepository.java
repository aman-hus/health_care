package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.DoctorAvailability;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    Optional<DoctorAvailability> findByHospital_IdAndDoctor_IdAndDate(Long hospitalId, Long doctorId, LocalDate date);
    List<DoctorAvailability> findByHospital_IdAndDoctor_IdAndDateBetween(Long hospitalId, Long doctorId, LocalDate start, LocalDate end);
    List<DoctorAvailability> findByHospital_IdAndDateAndAvailableTrue(Long hospitalId, LocalDate date);
}
