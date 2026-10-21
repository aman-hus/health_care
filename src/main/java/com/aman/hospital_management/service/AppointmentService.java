package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.AppointmentRequest;
import com.aman.hospital_management.dto.AppointmentResponse;
import com.aman.hospital_management.model.AppUser;
import com.aman.hospital_management.model.Appointment;
import com.aman.hospital_management.model.AppointmentStatus;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.repository.AppointmentRepository;
import com.aman.hospital_management.repository.UserRepository;
import com.aman.hospital_management.repository.HospitalRepository;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.security.HospitalContext;
import com.aman.hospital_management.model.Hospital;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalMembershipRepository membershipRepository;

    @Transactional
    public AppointmentResponse book(String email, Role callerRole, AppointmentRequest request) {
        if (callerRole == Role.NURSE) {
            throw new AccessDeniedException("Nurses cannot book appointments.");
        }
        AppUser caller = userByEmail(email);
        if (callerRole != Role.ADMIN && callerRole != Role.PATIENT) {
            throw new AccessDeniedException("Only admins and patients can book appointments.");
        }
        if (request.appointmentAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment time must be in the future.");
        }
        AppUser patient;
        if (callerRole == Role.PATIENT) {
            if (request.patientId() != null && !request.patientId().equals(caller.getId())) {
                throw new AccessDeniedException("Patients can only book appointments for themselves.");
            }
            patient = caller;
        } else {
            if (request.patientId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required when an admin books.");
            }
            patient = userById(request.patientId(), Role.PATIENT);
        }

        AppUser doctor = userById(request.doctorId(), Role.DOCTOR);
        AppUser nurse = request.nurseId() == null ? null : userById(request.nurseId(), Role.NURSE);
        Long hospitalId = HospitalContext.requireHospitalId();
        ensureMember(hospitalId, patient);
        ensureMember(hospitalId, doctor);
        if (nurse != null) ensureMember(hospitalId, nurse);
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found."));
        Appointment appointment = appointmentRepository.save(new Appointment(
                hospital, patient, doctor, nurse, request.appointmentAt(), request.reason()));
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMine(String email, Role role) {
        AppUser user = userByEmail(email);
        List<Appointment> appointments = switch (role) {
            case DOCTOR -> appointmentRepository.findByHospital_IdAndDoctor_IdOrderByAppointmentAtAsc(HospitalContext.requireHospitalId(), user.getId());
            case NURSE -> {
                LocalDate today = LocalDate.now();
                yield appointmentRepository.findByHospital_IdAndNurse_IdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(
                        HospitalContext.requireHospitalId(), user.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            }
            case PATIENT -> appointmentRepository.findByHospital_IdAndPatient_IdOrderByAppointmentAtAsc(HospitalContext.requireHospitalId(), user.getId());
            default -> throw new AccessDeniedException("This role cannot use the personal appointments view.");
        };
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findByHospital_IdOrderByAppointmentAtAsc(HospitalContext.requireHospitalId()).stream()
                .map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getForPatient(Long patientId) {
        userById(patientId, Role.PATIENT);
        return appointmentRepository.findByHospital_IdAndPatient_IdOrderByAppointmentAtAsc(HospitalContext.requireHospitalId(), patientId).stream()
                .map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getForDoctor(Long doctorId) {
        userById(doctorId, Role.DOCTOR);
        return appointmentRepository.findByHospital_IdAndDoctor_IdOrderByAppointmentAtAsc(HospitalContext.requireHospitalId(), doctorId).stream()
                .map(AppointmentResponse::from).toList();
    }

    @Transactional
    public AppointmentResponse accept(String email, Role callerRole, Long appointmentId) {
        Appointment appointment = appointmentForDoctor(email, callerRole, appointmentId);
        requirePending(appointment);
        appointment.accept();
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse reject(String email, Role callerRole, Long appointmentId) {
        Appointment appointment = appointmentForDoctor(email, callerRole, appointmentId);
        requirePending(appointment);
        appointment.reject();
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse reschedule(String email, Role callerRole, Long appointmentId,
                                          LocalDateTime appointmentAt) {
        if (callerRole != Role.PATIENT && callerRole != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the appointment's patient or doctor can reschedule it.");
        }
        if (appointmentAt == null || !appointmentAt.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment time must be in the future.");
        }
        AppUser caller = userByEmail(email);
        Appointment appointment = appointmentRepository.findByIdAndHospital_Id(appointmentId, HospitalContext.requireHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found: " + appointmentId));
        boolean isOwner = callerRole == Role.PATIENT
                ? appointment.getPatient().getId().equals(caller.getId())
                : appointment.getDoctor().getId().equals(caller.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only reschedule appointments associated with your account.");
        }
        if (appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A rejected appointment cannot be rescheduled.");
        }
        appointment.reschedule(appointmentAt);
        Appointment updatedAppointment = appointmentRepository.saveAndFlush(appointment);
        return AppointmentResponse.from(updatedAppointment);
    }

    private Appointment appointmentForDoctor(String email, Role callerRole, Long appointmentId) {
        if (callerRole != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned doctor can decide an appointment.");
        }
        AppUser caller = userByEmail(email);
        Appointment appointment = appointmentRepository.findByIdAndHospital_Id(appointmentId, HospitalContext.requireHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found: " + appointmentId));
        if (!appointment.getDoctor().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only decide appointments assigned to you.");
        }
        return appointment;
    }

    private void requirePending(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only pending appointments can be accepted or rejected.");
        }
    }

    @Transactional
    public void delete(String email, Role callerRole, Long appointmentId) {
        if (callerRole != Role.PATIENT && callerRole != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the appointment's patient or doctor can delete it.");
        }

        AppUser caller = userByEmail(email);
        Appointment appointment = appointmentRepository.findByIdAndHospital_Id(appointmentId, HospitalContext.requireHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found: " + appointmentId));

        boolean isOwner = callerRole == Role.PATIENT
                ? appointment.getPatient().getId().equals(caller.getId())
                : appointment.getDoctor().getId().equals(caller.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only delete appointments associated with your account.");
        }

        appointmentRepository.delete(appointment);
    }

    private AppUser userByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private AppUser userById(Long id, Role requiredRole) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
        Long hospitalId = HospitalContext.requireHospitalId();
        var membership = membershipRepository.findByHospital_IdAndUser_IdAndEnabledTrue(hospitalId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in this hospital."));
        if (membership.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User " + id + " must have role " + requiredRole + " in this hospital.");
        }
        return user;
    }

    private void ensureMember(Long hospitalId, AppUser user) {
        if (!membershipRepository.existsByHospital_IdAndUser_IdAndEnabledTrue(hospitalId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in this hospital.");
        }
    }
}
