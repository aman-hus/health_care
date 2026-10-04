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




    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {

            throw new EmailAlreadyExistsException(
                    "User with this email already exists"
            );
        }


        String encodedPassword =
                passwordEncoder.encode(request.getPassword());


        AppUser user = new AppUser(
                request.getName(),
                request.getEmail(),
                encodedPassword,
                request.getRole()
        );


        AppUser savedUser =
                userRepository.save(user);


        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isEnabled()
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
                Role.ADMIN
        );
        AppUser savedAdmin = userRepository.save(admin);
        return new UserResponse(
                savedAdmin.getId(), savedAdmin.getName(), savedAdmin.getEmail(),
                savedAdmin.getRole(), savedAdmin.isEnabled()
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


        String token =
                jwtService.generateToken(userDetails);


        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
