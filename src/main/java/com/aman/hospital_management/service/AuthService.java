package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.AuthResponse;
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

import org.springframework.stereotype.Service;
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
