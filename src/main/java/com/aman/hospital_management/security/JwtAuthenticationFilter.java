package com.aman.hospital_management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.aman.hospital_management.repository.HospitalMembershipRepository;
import com.aman.hospital_management.security.HospitalContext;
import com.aman.hospital_management.service.TokenRevocationService;

import java.io.IOException;

/** Authenticates API requests that include an Authorization: Bearer token. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final HospitalMembershipRepository membershipRepository;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HospitalMembershipRepository membershipRepository,
            TokenRevocationService tokenRevocationService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.membershipRepository = membershipRepository;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Parsing verifies the signature and rejects expired tokens.
            Claims claims = jwtService.extractClaims(token);
            if (!tokenRevocationService.isRevoked(token)) {
                String email = claims.getSubject();
                Long hospitalId = claims.get("hospitalId", Long.class);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (hospitalId == null) {
                        boolean platformAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
                        if (platformAdmin && userDetails.isEnabled()) {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    } else {
                        var membership = membershipRepository.findByHospital_IdAndUser_EmailAndEnabledTrue(hospitalId, email)
                                .orElseThrow(() -> new UsernameNotFoundException("No active membership for selected hospital."));
                        if (userDetails.isEnabled() && email.equals(userDetails.getUsername()) && membership.getHospital().isEnabled()) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                    "ROLE_" + membership.getRole().name()))
                                    );
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            HospitalContext.set(hospitalId);
                        }
                    }
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            // Invalid or expired credentials remain unauthenticated; the security chain
            // will return 401 for protected routes and still allow public routes through.
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            HospitalContext.clear();
        }
    }
}
