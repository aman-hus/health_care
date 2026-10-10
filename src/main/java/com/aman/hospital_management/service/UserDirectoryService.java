package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.UserDirectoryResponse;
import com.aman.hospital_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDirectoryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDirectoryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDirectoryResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.isEnabled(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ))
                .toList();
    }
}
