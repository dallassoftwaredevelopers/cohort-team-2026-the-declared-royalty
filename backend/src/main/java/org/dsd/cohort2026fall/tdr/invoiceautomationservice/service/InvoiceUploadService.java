package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.DuplicateFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.StorageException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg"
    );

    private final InvoiceRepository invoiceRepository;
    private final DocumentStorageService storageService;
    private final AuditLogService auditLogService;
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

                auditLogService.record(
                        saved.getInvoiceId(),
                        systemUser().getUserId(),
                        AuditLogCreateRequest.ActionTypeEnum.SYSTEM_INGESTED,
                        "File uploaded and stored",
                        "NONE",
                        STATUS_INGESTED,
                        "File: " + filename + " | storage key: " + key + " | sha256: " + hash);

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

    public record InvoiceFile(InputStream content, String contentType, String filename) {}

    /** Opens the invoice's original file. The caller must close the stream. */
    public InvoiceFile loadFile(UUID invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        String key = invoice.getBlobFileUrl();
        if (key == null || key.isBlank()) {
            throw new ResourceNotFoundException("Invoice has no file: " + invoiceId);
        }

        String contentType = CONTENT_TYPES.getOrDefault(extension(key), "application/octet-stream");
        String filename = invoice.getOriginalFilename() != null
                ? invoice.getOriginalFilename()
                : key.substring(key.lastIndexOf('/') + 1);

        return new InvoiceFile(storageService.load(key), contentType, filename);
    }

    private void recordDuplicateAttempt(InvoiceEntity original, String filename) {
        try {
            auditLogService.record(
                    original.getInvoiceId(),
                    systemUser().getUserId(),
                    AuditLogCreateRequest.ActionTypeEnum.SYSTEM_INGESTED,
                    "Duplicate upload rejected",
                    original.getInvoiceStatus(),
                    original.getInvoiceStatus(),
                    "Rejected upload of '" + filename + "': identical to this invoice's file");
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

    /** Returns which of the given hashes already belong to an uploaded invoice. */
    public List<String> findExistingHashes(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) return List.of();
        return invoiceRepository.findExistingHashes(new HashSet<>(hashes));
    }

    private String extension(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "" : key.substring(dot + 1).toLowerCase();
    }
}