package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.UserDirectoryResponse;
import com.aman.hospital_management.service.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserDirectoryService userDirectoryService;

    @GetMapping
    public List<UserDirectoryResponse> getAllUsers() {
        return userDirectoryService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserDirectoryResponse getUserById(@PathVariable Long id) {
        return userDirectoryService.getUserById(id);
    }
}
