package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.HospitalMembership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HospitalMembershipRepository extends JpaRepository<HospitalMembership, Long> {
    @EntityGraph(attributePaths = "hospital")
    Optional<HospitalMembership> findByHospital_IdAndUser_EmailAndEnabledTrue(Long hospitalId, String email);
    @EntityGraph(attributePaths = "hospital")
    List<HospitalMembership> findByUser_EmailAndEnabledTrue(String email);
    boolean existsByHospital_IdAndUser_IdAndEnabledTrue(Long hospitalId, Long userId);
    List<HospitalMembership> findByHospital_IdAndEnabledTrueOrderByUser_NameAsc(Long hospitalId);
    Optional<HospitalMembership> findByHospital_IdAndUser_IdAndEnabledTrue(Long hospitalId, Long userId);
}
