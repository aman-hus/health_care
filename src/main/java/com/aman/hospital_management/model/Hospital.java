package com.aman.hospital_management.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospitals", uniqueConstraints = @UniqueConstraint(name = "uk_hospitals_code", columnNames = "code"))
public class Hospital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, length = 80)
    private String code;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Hospital() { }
    public Hospital(String name, String code) { this.name = name; this.code = code; }
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
