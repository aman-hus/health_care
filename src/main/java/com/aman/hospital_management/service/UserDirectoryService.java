package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.UserDirectoryResponse;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.repository.UserRepository;
import com.aman.hospital_management.repository.DoctorAvailabilityRepository;
import com.aman.hospital_management.repository.HospitalRepository;
import com.aman.hospital_management.model.DoctorAvailability;
import com.aman.hospital_management.dto.MyProfileResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityRequest;
import com.aman.hospital_management.dto.DoctorDateAvailabilityResponse;
import com.aman.hospital_management.dto.DoctorAvailabilityScheduleRequest;
import com.aman.hospital_management.security.HospitalContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDirectoryService {

    private final HospitalMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new MyProfileResponse(user.getId(), user.getName(), user.getEmail(),
                user.getProfileImageUrl(), user.getSpecialization(), user.getMrnNumber());
    }

    @Transactional
    public DoctorDateAvailabilityResponse updateDoctorAvailability(String email, java.time.LocalDate date, boolean available,
            java.time.LocalTime workingStartTime, java.time.LocalTime workingEndTime) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Long hospitalId = HospitalContext.requireHospitalId();
        var membership = membershipRepository.findByHospital_IdAndUser_IdAndEnabledTrue(hospitalId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in this hospital."));
        if (membership.getRole() != com.aman.hospital_management.model.Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctors can update their appointment availability.");
        }
        if (date.isBefore(java.time.LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability date cannot be in the past.");
        }
        if (available && (workingStartTime == null || workingEndTime == null || !workingEndTime.isAfter(workingStartTime))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Working hours with an end time after the start time are required when marking a date available.");
        }
        var hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found."));
        var availability = doctorAvailabilityRepository.findByHospital_IdAndDoctor_IdAndDate(
                hospitalId, user.getId(), date).orElseGet(() -> new DoctorAvailability(hospital, user, date,
                        false, workingStartTime, workingEndTime));
        availability.setAvailable(available);
        if (workingStartTime != null && workingEndTime != null) availability.setWorkingHours(workingStartTime, workingEndTime);
        doctorAvailabilityRepository.save(availability);
        return new DoctorDateAvailabilityResponse(user.getId(), date, available,
                available ? "Doctor marked available for this date." : "Doctor marked unavailable for this date.");
    }

    @Transactional
    public Map<String, Object> setDoctorAvailabilitySchedule(String email, DoctorAvailabilityScheduleRequest request) {
        var doctor = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Long hospitalId = HospitalContext.requireHospitalId();
        var membership = membershipRepository.findByHospital_IdAndUser_IdAndEnabledTrue(hospitalId, doctor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in this hospital."));
        if (membership.getRole() != com.aman.hospital_management.model.Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctors can update their availability schedule.");
        }
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(2);
        if (request.startDate().isBefore(today) || request.endDate().isBefore(request.startDate())
                || request.endDate().isAfter(maxDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Schedule dates must be from today through " + maxDate + " and span no more than two months.");
        }
        if (request.workingEndTime().isBefore(request.workingStartTime().plusMinutes(20))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Working hours must allow at least one 20-minute appointment slot.");
        }
        Set<LocalDate> availableDates = request.availableDates() == null ? Set.of() : request.availableDates();
        Set<LocalDate> unavailableDates = request.unavailableDates() == null ? Set.of() : request.unavailableDates();
        if (availableDates.stream().anyMatch(date -> date.isBefore(request.startDate()) || date.isAfter(request.endDate()))
                || unavailableDates.stream().anyMatch(date -> date.isBefore(request.startDate()) || date.isAfter(request.endDate()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every date override must be inside the schedule range.");
        }
        if (availableDates.stream().anyMatch(unavailableDates::contains)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A date cannot be both available and unavailable.");
        }

        var hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found."));
        var existing = doctorAvailabilityRepository.findByHospital_IdAndDoctor_IdAndDateBetween(
                hospitalId, doctor.getId(), request.startDate(), request.endDate()).stream()
                .collect(Collectors.toMap(com.aman.hospital_management.model.DoctorAvailability::getDate, Function.identity()));
        var updates = new java.util.ArrayList<com.aman.hospital_management.model.DoctorAvailability>();
        for (LocalDate date = request.startDate(); !date.isAfter(request.endDate()); date = date.plusDays(1)) {
            boolean available = (request.availableDays().contains(date.getDayOfWeek()) || availableDates.contains(date))
                    && !unavailableDates.contains(date);
            var day = existing.get(date);
            if (day == null) day = new com.aman.hospital_management.model.DoctorAvailability(hospital, doctor, date,
                    available, request.workingStartTime(), request.workingEndTime());
            else {
                day.setAvailable(available);
                day.setWorkingHours(request.workingStartTime(), request.workingEndTime());
            }
            updates.add(day);
        }
        doctorAvailabilityRepository.saveAll(updates);
        return Map.ofEntries(
                Map.entry("doctorId", doctor.getId()),
                Map.entry("startDate", request.startDate()),
                Map.entry("endDate", request.endDate()),
                Map.entry("availableDays", request.availableDays()),
                Map.entry("availableDates", availableDates),
                Map.entry("unavailableDates", unavailableDates),
                Map.entry("workingStartTime", request.workingStartTime()),
                Map.entry("workingEndTime", request.workingEndTime()),
                Map.entry("appointmentDurationMinutes", 20),
                Map.entry("breakBetweenAppointmentsMinutes", 10),
                Map.entry("updatedDates", updates.size()));
    }

    @Transactional(readOnly = true)
    public List<UserDirectoryResponse> getAllUsers() {
        return membershipRepository.findByHospital_IdAndEnabledTrueOrderByUser_NameAsc(HospitalContext.requireHospitalId()).stream()
                .map(membership -> {
                    var user = membership.getUser();
                    return new UserDirectoryResponse(user.getId(), user.getName(), user.getEmail(), membership.getRole(),
                            user.isEnabled(), user.getContactNumber(), user.getGender(), user.getDateOfBirth(),
                            user.getProfileImageUrl(), user.getSpecialization(),
                            user.getMrnNumber(),
                            user.getCreatedAt(), user.getUpdatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDirectoryResponse getUserById(Long id) {
        var membership = membershipRepository.findByHospital_IdAndUser_IdAndEnabledTrue(HospitalContext.requireHospitalId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        var user = membership.getUser();

        return new UserDirectoryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                membership.getRole(),
                user.isEnabled(),
                user.getContactNumber(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getProfileImageUrl(),
                user.getSpecialization(),
                user.getMrnNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}
