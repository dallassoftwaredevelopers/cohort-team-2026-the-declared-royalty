package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import lombok.RequiredArgsConstructor;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.InvoiceUploadsApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceUploadResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.InvoiceUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class InvoiceUploadController implements InvoiceUploadsApi {

    private final InvoiceUploadService invoiceUploadService;

    @Override
    public ResponseEntity<InvoiceUploadResponse> uploadInvoice(MultipartFile file) {
        InvoiceEntity saved = invoiceUploadService.upload(file);

        InvoiceUploadResponse body = new InvoiceUploadResponse();
        body.setInvoiceId(saved.getInvoiceId());
        body.setStatus(saved.getInvoiceStatus());
        body.setOriginalFilename(saved.getOriginalFilename());
        body.setCreatedAt(saved.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}