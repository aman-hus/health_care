package com.aman.hospital_management.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_patient", columnList = "patient_id"),
        @Index(name = "idx_appointment_doctor", columnList = "doctor_id"),
        @Index(name = "idx_appointment_nurse", columnList = "nurse_id")
})
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private AppUser patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private AppUser doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id")
    private AppUser nurse;

    @Column(nullable = false)
    private LocalDateTime appointmentAt;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Appointment() { }

    public Appointment(Hospital hospital, AppUser patient, AppUser doctor, AppUser nurse,
                       LocalDateTime appointmentAt, String reason) {
        this.hospital = hospital;
        this.patient = patient;
        this.doctor = doctor;
        this.nurse = nurse;
        this.appointmentAt = appointmentAt;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Hospital getHospital() { return hospital; }
    public AppUser getPatient() { return patient; }
    public AppUser getDoctor() { return doctor; }
    public AppUser getNurse() { return nurse; }
    public LocalDateTime getAppointmentAt() { return appointmentAt; }
    public void reschedule(LocalDateTime appointmentAt) { this.appointmentAt = appointmentAt; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
    public void accept() { this.status = AppointmentStatus.SCHEDULED; }
    public void reject() { this.status = AppointmentStatus.REJECTED; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
