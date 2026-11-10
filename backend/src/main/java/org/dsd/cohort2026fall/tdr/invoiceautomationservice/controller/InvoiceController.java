package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.InvoicesApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.InvoiceService;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class InvoiceController implements InvoicesApi {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public ResponseEntity<InvoiceResponse> getInvoiceByUuid(UUID invoiceUuid) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceUuid));
    }

    @Override
    public ResponseEntity<InvoicePageResponse> getAllInvoices(Integer page, Integer size, List<InvoiceStatus> status, String q) {
        return ResponseEntity.ok(invoiceService.getInvoices(page, size, status, q));
    }

    @Override
    public ResponseEntity<InvoiceSummaryResponse> getInvoiceSummary(OffsetDateTime receivedSince) {
        return ResponseEntity.ok(invoiceService.getSummary(receivedSince));
    }

    @Override
    public ResponseEntity<InvoiceResponse> createInvoice(InvoiceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request));
    }

    @Override
    public ResponseEntity<InvoiceResponse> patchInvoice(UUID invoiceUuid, InvoiceUpdateRequest invoiceUpdateRequest) {
        return ResponseEntity.ok(invoiceService.updateInvoice(invoiceUuid, invoiceUpdateRequest));
    }
}