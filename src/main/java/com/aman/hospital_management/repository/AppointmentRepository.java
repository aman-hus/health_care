package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import com.aman.hospital_management.model.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByHospital_IdOrderByAppointmentAtAsc(Long hospitalId);
    List<Appointment> findByHospital_IdAndDoctor_IdOrderByAppointmentAtAsc(Long hospitalId, Long doctorId);
    List<Appointment> findByHospital_IdAndNurse_IdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(
            Long hospitalId, Long nurseId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByHospital_IdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(
            Long hospitalId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByHospital_IdAndPatient_IdOrderByAppointmentAtAsc(Long hospitalId, Long patientId);
    java.util.Optional<Appointment> findByIdAndHospital_Id(Long id, Long hospitalId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :appointmentId and a.hospital.id = :hospitalId")
    java.util.Optional<Appointment> findByIdAndHospitalIdForUpdate(
            @Param("appointmentId") Long appointmentId, @Param("hospitalId") Long hospitalId);
    boolean existsByHospital_IdAndDoctor_IdAndAppointmentAtAndStatusIn(
            Long hospitalId, Long doctorId, LocalDateTime appointmentAt, List<AppointmentStatus> statuses);
    boolean existsByHospital_IdAndDoctor_IdAndAppointmentAtAndStatusInAndIdNot(
            Long hospitalId, Long doctorId, LocalDateTime appointmentAt, List<AppointmentStatus> statuses, Long appointmentId);
    List<Appointment> findByHospital_IdAndDoctor_IdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanAndStatusIn(
            Long hospitalId, Long doctorId, LocalDateTime start, LocalDateTime end, List<AppointmentStatus> statuses);
}
