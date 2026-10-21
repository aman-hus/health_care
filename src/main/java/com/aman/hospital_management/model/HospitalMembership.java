package com.aman.hospital_management.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hospital_memberships", uniqueConstraints = @UniqueConstraint(name = "uk_hospital_membership", columnNames = {"hospital_id", "user_id"}))
public class HospitalMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private Role role;
    @Column(nullable = false)
    private boolean enabled = true;

    protected HospitalMembership() { }
    public HospitalMembership(Hospital hospital, AppUser user, Role role) {
        this.hospital = hospital; this.user = user; this.role = role;
    }
    public Long getId() { return id; }
    public Hospital getHospital() { return hospital; }
    public AppUser getUser() { return user; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public void setRole(Role role) { this.role = role; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
