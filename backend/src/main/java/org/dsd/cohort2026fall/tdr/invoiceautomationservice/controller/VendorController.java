package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.VendorsApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorUpdateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.VendorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class VendorController implements VendorsApi {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @Override
    public ResponseEntity<List<VendorResponse>> getVendors() {
        return vendorService.getVendors();
    }

    @Override
    public ResponseEntity<VendorResponse> createVendor(VendorCreateRequest request) {
        return vendorService.createVendor(request);
    }

    @Override
    public ResponseEntity<VendorResponse> getVendorByUuid(UUID vendorUuid) {
        return vendorService.getVendorByUuid(vendorUuid);
    }

    @Override
    public ResponseEntity<VendorResponse> updateVendor(UUID vendorUuid, VendorUpdateRequest request) {
        return vendorService.updateVendor(vendorUuid, request);
    }
}
