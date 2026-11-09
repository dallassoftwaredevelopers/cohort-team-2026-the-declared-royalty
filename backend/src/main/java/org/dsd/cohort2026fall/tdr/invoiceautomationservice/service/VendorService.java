package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.VendorEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PaymentMethod;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorStatus;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorUpdateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class VendorService {
    private final VendorRepository vendorRepository;
    private static final String MISSING_VALUE = "NULL";

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public ResponseEntity<List<VendorResponse>> getVendors() {
        List<VendorResponse> vendors = vendorRepository.findAll().stream()
                .map(VendorEntity::toResponse)
                .toList();
        return ResponseEntity.ok(vendors);
    }

    public ResponseEntity<VendorResponse> getVendorByUuid(UUID vendorUuid) {
        return ResponseEntity.ok(findVendor(vendorUuid).toResponse());
    }

    public ResponseEntity<VendorResponse> createVendor(VendorCreateRequest request) {
        VendorStatus status = request.getStatus() != null ? request.getStatus() : VendorStatus.ACTIVE;
        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.ACH;
        OffsetDateTime now = now();

        VendorEntity entity = VendorEntity.builder()
                .vendorId(UUID.randomUUID())
                .name(request.getName())
                .phoneNumber(toStored(request.getPhoneNumber()))
                .status(status.getValue())
                .paymentUrl(toStored(request.getPaymentUrl()))
                .paymentMethod(paymentMethod.getValue())
                .createdAt(now)
                .updatedAt(now)
                .build();
        VendorEntity saved = vendorRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse());
    }

    public ResponseEntity<VendorResponse> updateVendor(UUID vendorUuid, VendorUpdateRequest request) {
        VendorEntity entity = findVendor(vendorUuid);

        // Partial update: only fields present in the request are changed; omitted (null) fields keep their value.
        // vendorId and createdAt are intentionally left untouched.
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (isProvided(request.getPhoneNumber())) {
            entity.setPhoneNumber(nullIfNullKeyword(request.getPhoneNumber()));
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().getValue());
        }
        if (isProvided(request.getPaymentUrl())) {
            entity.setPaymentUrl(nullIfNullKeyword(request.getPaymentUrl()));
        }
        if (request.getPaymentMethod() != null) {
            entity.setPaymentMethod(request.getPaymentMethod().getValue());
        }
        entity.setUpdatedAt(now());

        VendorEntity saved = vendorRepository.save(entity);
        return ResponseEntity.ok(saved.toResponse());
    }

    // Missing values use the legacy placeholder; the explicit NULL keyword clears a nullable column.
    private String toStored(String value) {
        return isProvided(value) ? nullIfNullKeyword(value) : MISSING_VALUE;
    }

    // Update: null or "" means the field was not provided and is left unchanged
    private boolean isProvided(String value) {
        return value != null && !value.isEmpty();
    }

    private String nullIfNullKeyword(String value) {
        return MISSING_VALUE.equalsIgnoreCase(value) ? null : value;
    }

    // Postgres stores microseconds; truncate so the response matches what later reads return
    private OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    private VendorEntity findVendor(UUID vendorUuid) {
        return vendorRepository.findById(vendorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorUuid));
    }
}
