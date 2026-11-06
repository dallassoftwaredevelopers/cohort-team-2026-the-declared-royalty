package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import lombok.RequiredArgsConstructor;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.InvoiceUploadsApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.DuplicateCheckRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.DuplicateCheckResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceUploadResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.InvoiceUploadService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    @Override
    public ResponseEntity<DuplicateCheckResponse> checkDuplicateUploads(DuplicateCheckRequest request) {
        DuplicateCheckResponse body = new DuplicateCheckResponse();
        body.setDuplicates(invoiceUploadService.findExistingHashes(request.getHashes()));
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Resource> getInvoiceFile(UUID invoiceId) {
        InvoiceUploadService.InvoiceFile file = invoiceUploadService.loadFile(invoiceId);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(file.content()));
    }
}