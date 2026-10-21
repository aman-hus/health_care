package com.aman.hospital_management.config;

import com.aman.hospital_management.security.CustomUserDetailsService;
import com.aman.hospital_management.security.JwtAuthenticationFilter;
import com.aman.hospital_management.security.JwtService;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final HospitalMembershipRepository membershipRepository;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            HospitalMembershipRepository membershipRepository
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.membershipRepository = membershipRepository;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://medicare-e20b9.web.app",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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


    /** Login and first-admin bootstrap are public; bootstrap closes after initial setup. */
    @Bean
    @Order(1)
    public SecurityFilterChain authEndpoints(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/**")
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/bootstrap-admin", "/api/auth/hospitals").permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiEndpoints(
            HttpSecurity http
    ) throws Exception {

        http
                .securityMatcher("/api/**")
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Unauthorized",
                                "Authentication is required to access this resource."
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeSecurityError(
                                response,
                                HttpStatus.FORBIDDEN,
                                "Forbidden",
                                "Your account does not have permission to access this resource."
                        )))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, userDetailsService, membershipRepository),
                        UsernamePasswordAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/nurse/**").hasRole("NURSE")
                        .requestMatchers("/api/medical-staff/**").hasRole("MEDICAL_STAFF")
                        .requestMatchers("/api/medical-records/**").hasAnyRole("DOCTOR", "NURSE")
                        .requestMatchers("/api/appointments/**").hasAnyRole("ADMIN", "NURSE", "DOCTOR", "PATIENT")
                        .requestMatchers("/api/management/**").hasAnyRole("ADMIN", "MEDICAL_STAFF")
                        .requestMatchers("/api/profile/**").hasRole("PATIENT")
                        .requestMatchers("/api/users/**").authenticated()
                        // New API routes must receive an explicit role policy before they are accessible.
                        .anyRequest().denyAll()
                );

        return http.build();
    }

    private void writeSecurityError(
            HttpServletResponse response,
            HttpStatus status,
            String error,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + error
                + "\",\"message\":\"" + message + "\"}");
    }

}
