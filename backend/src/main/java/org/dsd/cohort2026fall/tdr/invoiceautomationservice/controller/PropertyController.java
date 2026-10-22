package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.PropertiesApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PropertyCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PropertyResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class PropertyController implements PropertiesApi {

    private final PropertyRepository propertyRepository;

    public PropertyController(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public ResponseEntity<List<PropertyResponse>> getProperties() {
        List<PropertyResponse> properties = propertyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(properties);
    }

    @Override
    public ResponseEntity<PropertyResponse> createProperty(PropertyCreateRequest request) {
        PropertyEntity entity = new PropertyEntity();
        entity.setName(request.getName());
        entity.setStreetAddress(request.getStreetAddress());
        entity.setCity(request.getCity());
        entity.setState(request.getState());
        entity.setZipCode(request.getZipCode());
        entity.setStatus("ACTIVE");

        PropertyEntity saved = propertyRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @Override
    public ResponseEntity<PropertyResponse> getPropertyById(UUID propertyId) {
        PropertyEntity entity = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        return ResponseEntity.ok(toResponse(entity));
    }

    private PropertyResponse toResponse(PropertyEntity entity) {
        PropertyResponse response = new PropertyResponse();
        response.setPropertyId(entity.getPropertyId());
        response.setName(entity.getName());
        response.setStreetAddress(entity.getStreetAddress());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setZipCode(entity.getZipCode());
        response.setStatus(entity.getStatus());
        return response;
    }
}

//test