package com.aman.hospital_management.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_availability", uniqueConstraints = @UniqueConstraint(
        name = "uk_doctor_availability_day", columnNames = {"hospital_id", "doctor_id", "availability_date"}))
public class DoctorAvailability {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private AppUser doctor;

    @Column(name = "availability_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "working_start_time")
    private LocalTime workingStartTime;

    @Column(name = "working_end_time")
    private LocalTime workingEndTime;

    protected DoctorAvailability() { }
    public DoctorAvailability(Hospital hospital, AppUser doctor, LocalDate date, boolean available,
                              LocalTime workingStartTime, LocalTime workingEndTime) {
        this.hospital = hospital;
        this.doctor = doctor;
        this.date = date;
        this.available = available;
        this.workingStartTime = workingStartTime;
        this.workingEndTime = workingEndTime;
    }
    public void setAvailable(boolean available) { this.available = available; }
    public void setWorkingHours(LocalTime start, LocalTime end) { this.workingStartTime = start; this.workingEndTime = end; }
    public boolean isAvailable() { return available; }
    public LocalTime getWorkingStartTime() { return workingStartTime; }
    public LocalTime getWorkingEndTime() { return workingEndTime; }
    public AppUser getDoctor() { return doctor; }
    public LocalDate getDate() { return date; }
}
