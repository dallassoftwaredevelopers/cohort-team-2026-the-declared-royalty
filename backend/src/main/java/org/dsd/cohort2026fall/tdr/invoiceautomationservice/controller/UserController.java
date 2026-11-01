package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.UsersApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.UserCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.UserResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.UserUpdateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class UserController implements UsersApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserResponse> createUser(UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Override
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @Override
    public ResponseEntity<UserResponse> getUserByUserId(UUID userId) {
        return ResponseEntity.ok(userService.getUserByUserId(userId));
    }

    @Override
    public ResponseEntity<UserResponse> updateUserByUserId(UUID userId, UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserByUserId(userId, request));
    }

    @Override
    public ResponseEntity<Void> deactivateUserByUserId(UUID userId) {
        userService.deactivateUserByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
