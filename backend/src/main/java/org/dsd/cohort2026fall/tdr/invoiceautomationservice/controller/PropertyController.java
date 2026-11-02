package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.PropertiesApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PropertyCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PropertyResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.PropertyRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.PropertyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class PropertyController implements PropertiesApi {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public ResponseEntity<List<PropertyResponse>> getProperties() {
        return ResponseEntity.ok(propertyService.getAllProperties());
    }

    @Override
    public ResponseEntity<PropertyResponse> createProperty(PropertyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyService.createProperty(request));
    }

    @Override
    public ResponseEntity<PropertyResponse> getPropertyById(UUID propertyId) {
        return ResponseEntity.ok(propertyService.getPropertyById(propertyId));
    }
}