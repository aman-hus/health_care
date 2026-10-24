package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.AuthResponse;
import com.aman.hospital_management.dto.BootstrapAdminRequest;
import com.aman.hospital_management.dto.LoginRequest;
import com.aman.hospital_management.dto.RegisterRequest;
import com.aman.hospital_management.exception.EmailAlreadyExistsException;
import com.aman.hospital_management.exception.UserNotFoundException;
import com.aman.hospital_management.model.AppUser;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.repository.UserRepository;
import com.aman.hospital_management.repository.HospitalRepository;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.model.Hospital;
import com.aman.hospital_management.model.HospitalMembership;
import com.aman.hospital_management.security.HospitalContext;
import com.aman.hospital_management.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.aman.hospital_management.dto.UserResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final HospitalRepository hospitalRepository;
    private final HospitalMembershipRepository membershipRepository;




    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (request.getRole() == Role.PLATFORM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform admin cannot be assigned through hospital user management.");
        }

        Hospital hospital = hospitalRepository.findById(HospitalContext.requireHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found."));
        AppUser savedUser = userRepository.findByEmail(request.getEmail()).orElseGet(() ->
                userRepository.save(new AppUser(request.getName(), request.getEmail(),
                        passwordEncoder.encode(request.getPassword()), request.getRole(),
                        request.getContactNumber(), request.getGender(), request.getDateOfBirth())));
        if (membershipRepository.existsByHospital_IdAndUser_IdAndEnabledTrue(hospital.getId(), savedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this hospital.");
        }
        membershipRepository.save(new HospitalMembership(hospital, savedUser, request.getRole()));


        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                request.getRole(),
                savedUser.isEnabled(),
                hospital.getId(),
                savedUser.getContactNumber(), savedUser.getGender(), savedUser.getDateOfBirth()
        );
    }

    /** Creates the first system administrator during initial setup. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserResponse bootstrapAdmin(BootstrapAdminRequest request) {
        if (userRepository.count() != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Initial admin setup is already complete"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("User with this email already exists");
        }

        AppUser admin = new AppUser(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Role.PLATFORM_ADMIN
        );
        AppUser savedAdmin = userRepository.save(admin);
        return new UserResponse(
                savedAdmin.getId(), savedAdmin.getName(), savedAdmin.getEmail(),
                savedAdmin.getRole(), savedAdmin.isEnabled(), null,
                savedAdmin.getContactNumber(), savedAdmin.getGender(), savedAdmin.getDateOfBirth()
        );
    }




    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );


        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();


        AppUser user =
                userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new UserNotFoundException("User not found")
                        );

        var memberships = membershipRepository.findByUser_EmailAndEnabledTrue(user.getEmail());
        if (user.getRole() == Role.PLATFORM_ADMIN && memberships.isEmpty() && request.getHospitalId() == null) {
            String token = jwtService.generateToken(userDetails);
            return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(),
                    Role.PLATFORM_ADMIN.name(), null, user.getProfileImageUrl());
        }
        var selected = request.getHospitalId() == null
                ? (memberships.size() == 1 ? memberships.get(0) : null)
                : memberships.stream().filter(m -> m.getHospital().getId().equals(request.getHospitalId())).findFirst().orElse(null);
        if (selected == null || !selected.getHospital().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    memberships.size() > 1 && request.getHospitalId() == null
                            ? "hospitalId is required when your account belongs to multiple hospitals."
                            : "You do not have an active membership in the selected hospital.");
        }


        String token =
                jwtService.generateToken(userDetails, selected.getHospital().getId());


        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                selected.getRole().name(),
                selected.getHospital().getId(),
                user.getProfileImageUrl()
        );
    }
}
