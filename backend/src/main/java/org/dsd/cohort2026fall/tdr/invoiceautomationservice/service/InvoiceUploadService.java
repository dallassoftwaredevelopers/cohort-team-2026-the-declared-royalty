package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.DuplicateFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.StorageException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.AuditLogRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceUploadService {

    public static final String STATUS_INGESTED = "INGESTED";

    // Seeded in V4. Stands in for the uploader until real authentication exists.
    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String DUPLICATE_MESSAGE = "Duplicate file: this file has already been uploaded";

    private static final Tika TIKA = new Tika();

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", "pdf",
            "image/png", "png",
            "image/jpeg", "jpg"
    );

    private final InvoiceRepository invoiceRepository;
    private final DocumentStorageService storageService;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public InvoiceEntity upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException("Could not read file");
        }

        // Check the actual file contents, not the Content-Type header, which the client controls
        String extension = ALLOWED_TYPES.get(TIKA.detect(bytes));
        if (extension == null) {
            throw new InvalidFileException("File type invalid: only PDF, PNG and JPEG are accepted");
        }

        String hash = sha256(bytes);
        String filename = cleanFilename(file.getOriginalFilename());

        Optional<InvoiceEntity> existing = invoiceRepository.findByBlobFileSha256(hash);
        if (existing.isPresent()) {
            recordDuplicateAttempt(existing.get(), filename);
            throw new DuplicateFileException(DUPLICATE_MESSAGE);
        }

        String key = storageService.store(file, extension);

        try {
            // Invoice row and its audit entry commit together or not at all
            return transactionTemplate.execute(status -> {
                InvoiceEntity saved = invoiceRepository.saveAndFlush(InvoiceEntity.builder()
                        .invoiceId(UUID.randomUUID())
                        .invoiceStatus(STATUS_INGESTED)
                        .blobFileUrl(key)
                        .blobFileSha256(hash)
                        .originalFilename(filename)
                        .build());

                auditLogRepository.save(AuditLogEntity.builder()
                        .auditUuid(UUID.randomUUID())
                        .invoice(saved)
                        .user(systemUser())
                        .actionType(AuditLogCreateRequest.ActionTypeEnum.SYSTEM_INGESTED)
                        .actionTaken("File uploaded and stored")
                        .fromState("NONE")
                        .toState(STATUS_INGESTED)
                        .notes("File: " + filename + " | storage key: " + key + " | sha256: " + hash)
                        .build());

                return saved;
            });
        } catch (DataIntegrityViolationException e) {
            safeDelete(key);
            // Same file uploaded at the same moment: the unique index caught it
            if (invoiceRepository.existsByBlobFileSha256(hash)) {
                throw new DuplicateFileException(DUPLICATE_MESSAGE);
            }
            throw e;
        } catch (RuntimeException e) {
            // Never leave a stored file with no invoice row pointing to it
            safeDelete(key);
            throw e;
        }
    }

    private void recordDuplicateAttempt(InvoiceEntity original, String filename) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    auditLogRepository.save(AuditLogEntity.builder()
                            .auditUuid(UUID.randomUUID())
                            .invoice(original)
                            .user(systemUser())
                            .actionType(AuditLogCreateRequest.ActionTypeEnum.SYSTEM_INGESTED)
                            .actionTaken("Duplicate upload rejected")
                            .fromState(original.getInvoiceStatus())
                            .toState(original.getInvoiceStatus())
                            .notes("Rejected upload of '" + filename + "': identical to this invoice's file")
                            .build()));
        } catch (RuntimeException e) {
            log.warn("Could not record duplicate upload attempt for invoice {}", original.getInvoiceId(), e);
        }
    }

    private UserEntity systemUser() {
        return userRepository.findById(SYSTEM_USER_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "System user " + SYSTEM_USER_ID + " missing: has migration V4 run?"));
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String cleanFilename(String name) {
        if (name == null || name.isBlank()) return "unknown";
        String base = name.replaceAll(".*[/\\\\]", "");  // strip any path the browser sent
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private void safeDelete(String key) {
        try {
            storageService.delete(key);
        } catch (StorageException e) {
            log.warn("Could not clean up orphaned file {}", key, e);
        }
    }
}