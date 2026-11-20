package com.aman.hospital_management.repository;

import com.aman.hospital_management.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

public interface UserRepository extends JpaRepository<AppUser,Long> {
    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByContactNumber(String contactNumber);

    Optional<AppUser> findByEmailAndDateOfBirth(String email, LocalDate dateOfBirth);

    List<AppUser> findByContactNumberAndDateOfBirth(String contactNumber, LocalDate dateOfBirth);

    boolean existsByEmail(String email);
}
