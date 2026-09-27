package com.aman.hospital_management.config;

import com.aman.hospital_management.security.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService
    ) {
        this.userDetailsService = userDetailsService;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
    }


    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }


    @Bean
    public AuthenticationManager authenticationManager() {

        return authentication ->
                authenticationProvider()
                        .authenticate(authentication);
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(
//            HttpSecurity http
//    ) throws Exception {
//
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // 1. Allow everyone to access Authentication APIs (Register/Login)
//                        .requestMatchers("/api/auth/**").permitAll()
//
//                        // 2. Admin Only Endpoints
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//
//                        // 3. Doctor Only Endpoints
//                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
//
//                        // 4. Nurse Only Endpoints
//                        .requestMatchers("/api/nurse/**").hasRole("NURSE")
//
//                        // 5. Shared Medical Access (Both Doctors and Nurses can access)
//                        .requestMatchers("/api/medical-records/**").hasAnyRole("DOCTOR", "NURSE")
//
//                        // 6. Medical Staff / Management Endpoints (Billing, Inventory, Scheduling)
//                        .requestMatchers("/api/management/**").hasAnyRole("ADMIN", "MEDICAL_STAFF")
//
//                        // 7. Patient Only Endpoints
//                        .requestMatchers("/api/patient/**").hasRole("PATIENT")
//
//                        // 8. Any other endpoint not listed above requires the user to just be logged in
//                        .anyRequest().authenticated()
//                );
//
//        return http.build();
//    }

}