package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderByAppointmentAtAsc();
    List<Appointment> findByDoctor_IdOrderByAppointmentAtAsc(Long doctorId);
    List<Appointment> findByNurse_IdOrderByAppointmentAtAsc(Long nurseId);
    List<Appointment> findByPatient_IdOrderByAppointmentAtAsc(Long patientId);
}
