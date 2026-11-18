package com.aman.hospital_management.dto;

import com.aman.hospital_management.model.Consultation;
import com.aman.hospital_management.model.ConsultationStatus;
import com.aman.hospital_management.model.ConsultationSection;
import java.time.Instant;
import java.util.Map;

public record ConsultationResponse(
        Long id,
        Long appointmentId,
        Long hospitalId,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        ConsultationStatus status,
        ConsultationSection nextSection,
        Map<String, Object> diagnosisData,
        Map<String, Object> evaluationData,
        Map<String, Object> carePlanData,
        Map<String, Object> feesData,
        Map<String, Object> prescriptionData,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt,
        long version
) {
    public static ConsultationResponse from(Consultation consultation) {
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getAppointment().getId(),
                consultation.getHospital().getId(),
                consultation.getPatient().getId(),
                consultation.getPatient().getName(),
                consultation.getDoctor().getId(),
                consultation.getDoctor().getName(),
                consultation.getStatus(),
                consultation.getNextSection(),
                consultation.getDiagnosisData(),
                consultation.getEvaluationData(),
                consultation.getCarePlanData(),
                consultation.getFeesData(),
                consultation.getPrescriptionData(),
                consultation.getStartedAt(),
                consultation.getCompletedAt(),
                consultation.getUpdatedAt(),
                consultation.getVersion()
        );
    }
}
