package com.aman.hospital_management.dto;

import com.aman.hospital_management.model.Appointment;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        Long nurseId,
        String nurseName,
        LocalDateTime appointmentAt,
        String reason,
        LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(), appointment.getPatient().getId(), appointment.getPatient().getName(),
                appointment.getDoctor().getId(), appointment.getDoctor().getName(),
                appointment.getNurse() == null ? null : appointment.getNurse().getId(),
                appointment.getNurse() == null ? null : appointment.getNurse().getName(),
                appointment.getAppointmentAt(), appointment.getReason(), appointment.getCreatedAt()
        );
    }
}
