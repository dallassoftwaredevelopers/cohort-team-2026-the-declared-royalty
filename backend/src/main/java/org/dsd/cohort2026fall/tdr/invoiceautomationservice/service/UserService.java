package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.UserCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.UserResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.UserUpdateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserCreateRequest request) {
        UserEntity entity = new UserEntity();
        entity.setUserId(UUID.randomUUID());
        entity.setUserName(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setRole(request.getRole() != null ? request.getRole() : "ADMIN");
        entity.setStatus(request.getIsActive() != null ? request.getIsActive() : "ACTIVE");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        UserEntity saved = userRepository.save(entity);
        return toResponse(saved);
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserByUserId(UUID userId) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(entity);
    }

    public UserResponse updateUserByUserId(UUID userId, UserUpdateRequest request) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getUsername() != null) {
            entity.setUserName(request.getUsername());
        }
        if (request.getEmail() != null) {
            entity.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            entity.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            entity.setStatus(request.getIsActive());
        }
        entity.setUpdatedAt(OffsetDateTime.now());

        UserEntity updated = userRepository.save(entity);
        return toResponse(updated);
    }

    public void deactivateUserByUserId(UUID userId) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        entity.setStatus("INACTIVE");
        entity.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(entity);
    }

    private UserResponse toResponse(UserEntity entity) {
        UserResponse response = new UserResponse();
        response.setUserId(entity.getUserId());
        response.setUsername(entity.getUserName());
        response.setEmail(entity.getEmail());
        response.setRole(entity.getRole());
        response.setIsActive(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
