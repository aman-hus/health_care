package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.AppointmentRequest;
import com.aman.hospital_management.dto.AppointmentResponse;
import com.aman.hospital_management.dto.DoctorDateAvailabilityResponse;
import com.aman.hospital_management.dto.AvailableDoctorResponse;
import com.aman.hospital_management.dto.DoctorTimeSlotResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityDayResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityPageResponse;
import com.aman.hospital_management.model.AppUser;
import com.aman.hospital_management.model.Appointment;
import com.aman.hospital_management.model.AppointmentStatus;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.repository.AppointmentRepository;
import com.aman.hospital_management.repository.UserRepository;
import com.aman.hospital_management.repository.HospitalRepository;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.repository.DoctorAvailabilityRepository;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private static final int APPOINTMENT_DURATION_MINUTES = 20;
    private static final int BREAK_BETWEEN_APPOINTMENTS_MINUTES = 10;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;

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
        ensureSlotAvailable(doctor, request.appointmentAt());
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
    public DoctorDateAvailabilityResponse checkAvailability(Long doctorId, LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability date must be today or in the future.");
        }
        userById(doctorId, Role.DOCTOR);
        boolean available = isDoctorAvailableOnDate(doctorId, date);
        return new DoctorDateAvailabilityResponse(doctorId, date, available,
                available ? "Doctor is available on this date."
                        : "Doctor has not marked this date as available.");
    }

    @Transactional(readOnly = true)
    public List<AvailableDoctorResponse> findAvailableDoctors(LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability date must be today or in the future.");
        }
        Long hospitalId = HospitalContext.requireHospitalId();
        return doctorAvailabilityRepository.findByHospital_IdAndDateAndAvailableTrue(hospitalId, date).stream()
                .map(availability -> availability.getDoctor())
                .filter(AppUser::isEnabled)
                .filter(doctor -> membershipRepository.findByHospital_IdAndUser_IdAndEnabledTrue(hospitalId, doctor.getId())
                        .map(membership -> membership.getRole() == Role.DOCTOR).orElse(false))
                .map(doctor -> new AvailableDoctorResponse(doctor.getId(), doctor.getName(), doctor.getSpecialization(), date))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorTimeSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability date must be today or in the future.");
        }
        userById(doctorId, Role.DOCTOR);
        var availability = doctorAvailabilityRepository.findByHospital_IdAndDoctor_IdAndDate(
                HospitalContext.requireHospitalId(), doctorId, date).orElse(null);
        if (availability == null || !availability.isAvailable()
                || availability.getWorkingStartTime() == null || availability.getWorkingEndTime() == null) {
            return List.of();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<AppointmentStatus> activeStatuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.SCHEDULED);
        var booked = appointmentRepository.findByHospital_IdAndDoctor_IdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanAndStatusIn(
                HospitalContext.requireHospitalId(), doctorId, dayStart, dayEnd, activeStatuses);
        var slots = new ArrayList<DoctorTimeSlotResponse>();
        int stepMinutes = APPOINTMENT_DURATION_MINUTES + BREAK_BETWEEN_APPOINTMENTS_MINUTES;
        for (LocalTime start = availability.getWorkingStartTime();
             !start.plusMinutes(APPOINTMENT_DURATION_MINUTES).isAfter(availability.getWorkingEndTime());
             start = start.plusMinutes(stepMinutes)) {
            LocalDateTime slotStart = date.atTime(start);
            LocalDateTime slotEnd = slotStart.plusMinutes(APPOINTMENT_DURATION_MINUTES);
            if (slotStart.isAfter(LocalDateTime.now()) && booked.stream().noneMatch(appointment -> {
                LocalDateTime appointmentEnd = appointment.getAppointmentAt().plusMinutes(APPOINTMENT_DURATION_MINUTES);
                return slotStart.isBefore(appointmentEnd) && appointment.getAppointmentAt().isBefore(slotEnd);
            })) {
                slots.add(new DoctorTimeSlotResponse(slotStart, slotEnd));
            }
        }
        return slots;
    }

    @Transactional(readOnly = true)
    public DoctorAvailabilityPageResponse getAvailabilityCalendar(Long doctorId, Pageable pageable) {
        userById(doctorId, Role.DOCTOR);
        Long hospitalId = HospitalContext.requireHospitalId();
        var result = doctorAvailabilityRepository.findByHospital_IdAndDoctor_Id(hospitalId, doctorId, pageable);
        var dates = result.getContent().stream()
                .map(day -> new DoctorAvailabilityDayResponse(day.getDate(), day.isAvailable(),
                        day.getWorkingStartTime(), day.getWorkingEndTime()))
                .toList();
        return new DoctorAvailabilityPageResponse(doctorId, dates, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
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
        ensureSlotAvailable(appointment.getDoctor(), appointmentAt, appointment.getId());
        appointment.reschedule(appointmentAt);
        Appointment updatedAppointment = appointmentRepository.saveAndFlush(appointment);
        return AppointmentResponse.from(updatedAppointment);
    }

    private void ensureSlotAvailable(AppUser doctor, LocalDateTime appointmentAt) {
        ensureSlotAvailable(doctor, appointmentAt, null);
    }

    private void ensureSlotAvailable(AppUser doctor, LocalDateTime appointmentAt, Long ignoredAppointmentId) {
        var availability = doctorAvailabilityRepository.findByHospital_IdAndDoctor_IdAndDate(
                HospitalContext.requireHospitalId(), doctor.getId(), appointmentAt.toLocalDate()).orElse(null);
        if (availability == null || !availability.isAvailable())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This doctor is not marked available on this date.");
        if (availability.getWorkingStartTime() == null || availability.getWorkingEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This doctor's working hours are not configured for this date.");
        }
        boolean isSlotStart = false;
        for (LocalTime start = availability.getWorkingStartTime();
             !start.plusMinutes(APPOINTMENT_DURATION_MINUTES).isAfter(availability.getWorkingEndTime());
             start = start.plusMinutes(APPOINTMENT_DURATION_MINUTES + BREAK_BETWEEN_APPOINTMENTS_MINUTES)) {
            if (appointmentAt.equals(appointmentAt.toLocalDate().atTime(start))) {
                isSlotStart = true;
                break;
            }
        }
        if (!isSlotStart) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested time is not a valid appointment slot.");
        }
        Long hospitalId = HospitalContext.requireHospitalId();
        List<AppointmentStatus> activeStatuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.SCHEDULED);
        LocalDateTime slotEnd = appointmentAt.plusMinutes(APPOINTMENT_DURATION_MINUTES);
        boolean occupied = appointmentRepository.findByHospital_IdAndDoctor_IdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanAndStatusIn(
                        hospitalId, doctor.getId(), appointmentAt.toLocalDate().atStartOfDay(),
                        appointmentAt.toLocalDate().plusDays(1).atStartOfDay(), activeStatuses).stream()
                .filter(appointment -> ignoredAppointmentId == null || !appointment.getId().equals(ignoredAppointmentId))
                .anyMatch(appointment -> appointmentAt.isBefore(appointment.getAppointmentAt().plusMinutes(APPOINTMENT_DURATION_MINUTES))
                        && appointment.getAppointmentAt().isBefore(slotEnd));
        if (occupied) throw new ResponseStatusException(HttpStatus.CONFLICT, "This appointment slot is already booked.");
    }

    private boolean isDoctorAvailableOnDate(Long doctorId, LocalDate date) {
        return doctorAvailabilityRepository.findByHospital_IdAndDoctor_IdAndDate(
                HospitalContext.requireHospitalId(), doctorId, date)
                .map(com.aman.hospital_management.model.DoctorAvailability::isAvailable)
                .orElse(false);
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
