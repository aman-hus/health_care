package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.AppointmentRequest;
import com.aman.hospital_management.dto.AppointmentResponse;
import com.aman.hospital_management.model.AppUser;
import com.aman.hospital_management.model.Appointment;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.repository.AppointmentRepository;
import com.aman.hospital_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppointmentResponse book(String email, Role callerRole, AppointmentRequest request) {
        AppUser caller = userByEmail(email);
        if (callerRole != Role.ADMIN && callerRole != Role.NURSE && callerRole != Role.PATIENT) {
            throw new AccessDeniedException("Only admins, nurses, and patients can book appointments.");
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required when an admin or nurse books.");
            }
            patient = userById(request.patientId(), Role.PATIENT);
        }

        AppUser doctor = userById(request.doctorId(), Role.DOCTOR);
        AppUser nurse = request.nurseId() == null ? null : userById(request.nurseId(), Role.NURSE);
        Appointment appointment = appointmentRepository.save(new Appointment(
                patient, doctor, nurse, request.appointmentAt(), request.reason()));
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMine(String email, Role role) {
        AppUser user = userByEmail(email);
        List<Appointment> appointments = switch (role) {
            case DOCTOR -> appointmentRepository.findByDoctor_IdOrderByAppointmentAtAsc(user.getId());
            case NURSE -> appointmentRepository.findByNurse_IdOrderByAppointmentAtAsc(user.getId());
            case PATIENT -> appointmentRepository.findByPatient_IdOrderByAppointmentAtAsc(user.getId());
            default -> throw new AccessDeniedException("This role cannot use the personal appointments view.");
        };
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findAllByOrderByAppointmentAtAsc().stream()
                .map(AppointmentResponse::from).toList();
    }

    private AppUser userByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private AppUser userById(Long id, Role requiredRole) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
        if (user.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User " + id + " must have role " + requiredRole + ".");
        }
        return user;
    }
}
