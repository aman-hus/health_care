package com.aman.hospital_management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "consultations")
@Getter
@Setter(AccessLevel.PROTECTED)
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private AppUser patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private AppUser doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationStatus status = ConsultationStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_section", nullable = false, length = 20)
    private ConsultationSection nextSection = ConsultationSection.DIAGNOSIS;

    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(AccessLevel.NONE)
    @Column(name = "diagnosis_data", columnDefinition = "jsonb")
    private Map<String, Object> diagnosisData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(AccessLevel.NONE)
    @Column(name = "evaluation_data", columnDefinition = "jsonb")
    private Map<String, Object> evaluationData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(AccessLevel.NONE)
    @Column(name = "care_plan_data", columnDefinition = "jsonb")
    private Map<String, Object> carePlanData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(AccessLevel.NONE)
    @Column(name = "fees_data", columnDefinition = "jsonb")
    private Map<String, Object> feesData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(AccessLevel.NONE)
    @Column(name = "prescription_data", columnDefinition = "jsonb")
    private Map<String, Object> prescriptionData;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Consultation() { }

    public Consultation(Appointment appointment) {
        this.appointment = appointment;
        this.hospital = appointment.getHospital();
        this.patient = appointment.getPatient();
        this.doctor = appointment.getDoctor();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        startedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void saveDiagnosis(Map<String, Object> data) {
        diagnosisData = validatedCopy(data, ConsultationSection.DIAGNOSIS.next());
    }

    public void saveEvaluation(Map<String, Object> data) {
        evaluationData = validatedCopy(data, ConsultationSection.EVALUATION.next());
    }

    public void saveCarePlan(Map<String, Object> data) {
        carePlanData = validatedCopy(data, ConsultationSection.CARE_PLAN.next());
    }

    public void saveFees(Map<String, Object> data) {
        feesData = validatedCopy(data, ConsultationSection.FEES.next());
    }

    public void savePrescription(Map<String, Object> data) {
        prescriptionData = validatedCopy(data, ConsultationSection.PRESCRIPTION);
    }

    public void attachDiagnosisDocument(Map<String, Object> data) {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Completed consultations cannot be edited.");
        }
        if (data == null) {
            throw new IllegalArgumentException("Diagnosis data must be a JSON object.");
        }
        diagnosisData = copy(data);
    }

    public void complete() {
        if (status == ConsultationStatus.COMPLETED) {
            throw new IllegalStateException("Consultation is already completed.");
        }
        status = ConsultationStatus.COMPLETED;
        completedAt = Instant.now();
    }

    private void advanceTo(ConsultationSection section) {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Completed consultations cannot be edited.");
        }
        if (section.ordinal() > nextSection.ordinal()) nextSection = section;
    }

    private Map<String, Object> validatedCopy(Map<String, Object> data, ConsultationSection nextSection) {
        advanceTo(nextSection);
        return copy(data);
    }

    private Map<String, Object> copy(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Consultation section data must be a JSON object.");
        }
        return (Map<String, Object>) deepCopyValue(data);
    }

    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), deepCopyValue(item)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(item -> copy.add(deepCopyValue(item)));
            return copy;
        }
        return value;
    }

    public Map<String, Object> getDiagnosisData() { return copyForRead(diagnosisData); }
    public Map<String, Object> getEvaluationData() { return copyForRead(evaluationData); }
    public Map<String, Object> getCarePlanData() { return copyForRead(carePlanData); }
    public Map<String, Object> getFeesData() { return copyForRead(feesData); }
    public Map<String, Object> getPrescriptionData() { return copyForRead(prescriptionData); }

    private Map<String, Object> copyForRead(Map<String, Object> data) {
        return data == null ? null : copy(data);
    }

}
