package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Optional<Consultation> findByIdAndHospital_Id(Long id, Long hospitalId);
    Optional<Consultation> findByAppointment_IdAndHospital_Id(Long appointmentId, Long hospitalId);
    boolean existsByAppointment_IdAndHospital_Id(Long appointmentId, Long hospitalId);
}
