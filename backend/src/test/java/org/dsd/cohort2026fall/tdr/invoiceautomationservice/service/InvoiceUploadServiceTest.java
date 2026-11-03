package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.DuplicateFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.AuditLogRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceUploadServiceTest {

    private static final String STORAGE_KEY = "invoices/test-key.pdf";

    // Real file signatures so Tika detects them the same way it does in production
    private static final byte[] PDF_BYTES =
            "%PDF-1.4\n%test invoice\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private DocumentStorageService storageService;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private InvoiceUploadService service;

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        service = new InvoiceUploadService(
                invoiceRepository, storageService, auditLogRepository, userRepository, transactionTemplate);
    }

    // ---------- happy paths ----------

    @Test
    void validPdf_isStoredSavedAndAudited() {
        stubSystemUser();
        when(storageService.store(any(), eq("pdf"))).thenReturn(STORAGE_KEY);
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = service.upload(pdf("brightwater.pdf"));

        assertThat(result.getInvoiceId()).isNotNull();
        assertThat(result.getInvoiceStatus()).isEqualTo(InvoiceUploadService.STATUS_INGESTED);
        assertThat(result.getBlobFileUrl()).isEqualTo(STORAGE_KEY);
        assertThat(result.getBlobFileSha256()).matches("[0-9a-f]{64}");
        assertThat(result.getOriginalFilename()).isEqualTo("brightwater.pdf");

        AuditLogEntity audit = captureAudit();
        assertThat(audit.getInvoice()).isSameAs(result);
        assertThat(audit.getActionType()).isEqualTo(AuditLogCreateRequest.ActionTypeEnum.SYSTEM_INGESTED);
        assertThat(audit.getActionTaken()).isEqualTo("File uploaded and stored");
        assertThat(audit.getToState()).isEqualTo(InvoiceUploadService.STATUS_INGESTED);
    }

    @Test
    void validPng_isAccepted() {
        stubSystemUser();
        when(storageService.store(any(), eq("png"))).thenReturn("invoices/test-key.png");
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = service.upload(
                new MockMultipartFile("file", "scan.png", "image/png", PNG_BYTES));

        assertThat(result.getBlobFileUrl()).isEqualTo("invoices/test-key.png");
    }

    // ---------- validation ----------

    @Test
    void emptyFile_isRejected_andNothingStored() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.upload(empty))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("empty");

        verifyNoInteractions(storageService);
    }

    @Test
    void textFileRenamedToPdf_isRejected() {
        // Name and Content-Type both claim PDF; the bytes say otherwise
        MockMultipartFile fake = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf",
                "just some text, not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.upload(fake))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File type invalid");

        verifyNoInteractions(storageService);
    }

    // ---------- duplicates ----------

    @Test
    void duplicateFile_isRejected_notStored_andAuditedOnOriginal() {
        stubSystemUser();
        InvoiceEntity original = InvoiceEntity.builder()
                .invoiceId(UUID.randomUUID())
                .invoiceStatus("PENDING_APPROVAL")
                .build();
        when(invoiceRepository.findByBlobFileSha256(anyString())).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.upload(pdf("again.pdf")))
                .isInstanceOf(DuplicateFileException.class);

        verifyNoInteractions(storageService);
        verify(invoiceRepository, never()).saveAndFlush(any());

        AuditLogEntity audit = captureAudit();
        assertThat(audit.getInvoice()).isSameAs(original);
        assertThat(audit.getActionTaken()).isEqualTo("Duplicate upload rejected");
        assertThat(audit.getFromState()).isEqualTo("PENDING_APPROVAL");
        assertThat(audit.getToState()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void simultaneousDuplicate_caughtByUniqueIndex_deletesStoredFile() {
        when(storageService.store(any(), eq("pdf"))).thenReturn(STORAGE_KEY);
        when(invoiceRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));
        when(invoiceRepository.existsByBlobFileSha256(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.upload(pdf("race.pdf")))
                .isInstanceOf(DuplicateFileException.class);

        verify(storageService).delete(STORAGE_KEY);
    }

    // ---------- failure cleanup ----------

    @Test
    void databaseFailure_deletesStoredFile() {
        when(storageService.store(any(), eq("pdf"))).thenReturn(STORAGE_KEY);
        when(invoiceRepository.saveAndFlush(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.upload(pdf("bill.pdf")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    void missingSystemUser_failsClearly_andDeletesStoredFile() {
        when(storageService.store(any(), eq("pdf"))).thenReturn(STORAGE_KEY);
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(InvoiceUploadService.SYSTEM_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(pdf("bill.pdf")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("V4");

        verify(storageService).delete(STORAGE_KEY);
    }

    // ---------- filename handling ----------

    @Test
    void browserPathInFilename_isStripped() {
        stubSystemUser();
        when(storageService.store(any(), eq("pdf"))).thenReturn(STORAGE_KEY);
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = service.upload(pdf("C:\\fakepath\\october-water.pdf"));

        assertThat(result.getOriginalFilename()).isEqualTo("october-water.pdf");
    }

    // ---------- helpers ----------

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", PDF_BYTES);
    }

    private void stubSystemUser() {
        when(userRepository.findById(InvoiceUploadService.SYSTEM_USER_ID))
                .thenReturn(Optional.of(UserEntity.builder().build()));
    }

    private AuditLogEntity captureAudit() {
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
