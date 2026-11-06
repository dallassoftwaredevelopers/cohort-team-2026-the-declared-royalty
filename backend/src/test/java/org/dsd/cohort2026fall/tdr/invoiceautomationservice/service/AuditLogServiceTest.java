package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.BadRequestException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest.ActionTypeEnum;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.AuditLogRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AuditLogService auditLogService;

    // ---------- record ----------

    @Test
    void record_validInput_savesEntityAndReturnsResponse() {
        stubInvoiceAndUserExist();
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLogResponse response = auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "File uploaded and stored", "NONE", "INGESTED", "sha256=abc");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();

        assertThat(saved.getAuditUuid()).isNotNull();
        assertThat(saved.getInvoice().getInvoiceId()).isEqualTo(INVOICE_ID);
        assertThat(saved.getUser().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getActionType()).isEqualTo(ActionTypeEnum.SYSTEM_INGESTED);
        assertThat(saved.getActionTaken()).isEqualTo("File uploaded and stored");
        assertThat(saved.getFromState()).isEqualTo("NONE");
        assertThat(saved.getToState()).isEqualTo("INGESTED");
        assertThat(saved.getNotes()).isEqualTo("sha256=abc");

        assertThat(response.getAuditLogId()).isEqualTo(saved.getAuditUuid());
        assertThat(response.getInvoice()).isEqualTo(INVOICE_ID);
        assertThat(response.getUser()).isEqualTo(USER_ID);
        assertThat(response.getActionType()).isEqualTo("SYSTEM_INGESTED");
    }

    @Test
    void record_nullNotes_isAllowed() {
        stubInvoiceAndUserExist();
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLogResponse response = auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.USER_APPROVED,
                "Approved", "PENDING_APPROVAL", "APPROVED", null);

        assertThat(response.getNotes()).isNull();
    }

    @Test
    void record_invoiceNotFound_throwsAndDoesNotSave() {
        when(invoiceRepository.existsById(INVOICE_ID)).thenReturn(false);

        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "action", "NONE", "INGESTED", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice");

        verify(auditLogRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void record_userNotFound_throwsAndDoesNotSave() {
        when(invoiceRepository.existsById(INVOICE_ID)).thenReturn(true);
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "action", "NONE", "INGESTED", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void record_nullActionType_throwsBadRequest() {
        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, null,
                "action", "NONE", "INGESTED", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("actionType");

        verifyNoInteractions(auditLogRepository, invoiceRepository, userRepository);
    }

    @Test
    void record_blankActionTaken_throwsBadRequest() {
        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "   ", "NONE", "INGESTED", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("actionTaken");

        verifyNoInteractions(auditLogRepository, invoiceRepository, userRepository);
    }

    @Test
    void record_nullFromState_throwsBadRequest() {
        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "action", null, "INGESTED", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fromState");
    }

    @Test
    void record_actionTakenTooLong_throwsBadRequest() {
        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "a".repeat(226), "NONE", "INGESTED", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("actionTaken");
    }

    @Test
    void record_notesTooLong_throwsBadRequest() {
        assertThatThrownBy(() -> auditLogService.record(INVOICE_ID, USER_ID, ActionTypeEnum.SYSTEM_INGESTED,
                "action", "NONE", "INGESTED", "a".repeat(3001)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("notes");
    }

    // ---------- createAuditLog ----------

    @Test
    void createAuditLog_mapsRequestFieldsThroughToEntity() {
        stubInvoiceAndUserExist();
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLogCreateRequest request = new AuditLogCreateRequest();
        request.setInvoice(INVOICE_ID);
        request.setUser(USER_ID);
        request.setActionType(ActionTypeEnum.USER_VALIDATED);
        request.setActionTaken("User validated totals");
        request.setFromState("PENDING_MAPPING");
        request.setToState("PENDING_APPROVAL");
        request.setNotes("Looks good");

        AuditLogResponse response = auditLogService.createAuditLog(request);

        assertThat(response.getActionType()).isEqualTo("USER_VALIDATED");
        assertThat(response.getActionTaken()).isEqualTo("User validated totals");
        assertThat(response.getFromState()).isEqualTo("PENDING_MAPPING");
        assertThat(response.getToState()).isEqualTo("PENDING_APPROVAL");
        assertThat(response.getNotes()).isEqualTo("Looks good");
    }

    // ---------- getAuditLogs ----------

    @Test
    void getAuditLogs_lowercaseActionTypeAndBlankStates_areNormalized() {
        auditLogService.getAuditLogs(INVOICE_ID, "user_approved", "  ", "");

        verify(auditLogRepository).findAuditLogsByFilters(INVOICE_ID, ActionTypeEnum.USER_APPROVED, null, null);
    }

    @Test
    void getAuditLogs_noFilters_passesAllNulls() {
        auditLogService.getAuditLogs(null, null, null, null);

        verify(auditLogRepository).findAuditLogsByFilters(null, null, null, null);
    }

    @Test
    void getAuditLogs_invalidActionType_throwsBadRequest() {
        assertThatThrownBy(() -> auditLogService.getAuditLogs(null, "NOT_A_REAL_TYPE", null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NOT_A_REAL_TYPE");

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void getAuditLogs_mapsEntitiesToResponses() {
        AuditLogEntity entity = sampleEntity();
        when(auditLogRepository.findAuditLogsByFilters(INVOICE_ID, null, null, null)).thenReturn(List.of(entity));

        List<AuditLogResponse> result = auditLogService.getAuditLogs(INVOICE_ID, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuditLogId()).isEqualTo(entity.getAuditUuid());
        assertThat(result.get(0).getCreatedAt()).isEqualTo(entity.getCreatedAt());
    }

    // ---------- getAuditLogById ----------

    @Test
    void getAuditLogById_found_returnsResponse() {
        AuditLogEntity entity = sampleEntity();
        when(auditLogRepository.findById(entity.getAuditUuid())).thenReturn(Optional.of(entity));

        AuditLogResponse response = auditLogService.getAuditLogById(entity.getAuditUuid());

        assertThat(response.getAuditLogId()).isEqualTo(entity.getAuditUuid());
        assertThat(response.getInvoice()).isEqualTo(INVOICE_ID);
    }

    @Test
    void getAuditLogById_notFound_throwsResourceNotFound() {
        UUID missing = UUID.randomUUID();
        when(auditLogRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getAuditLogById(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }

    // ---------- helpers ----------

    private void stubInvoiceAndUserExist() {
        when(invoiceRepository.existsById(INVOICE_ID)).thenReturn(true);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(invoiceRepository.getReferenceById(INVOICE_ID))
                .thenReturn(InvoiceEntity.builder().invoiceId(INVOICE_ID).build());
        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(UserEntity.builder().userId(USER_ID).build());
    }

    private AuditLogEntity sampleEntity() {
        OffsetDateTime now = OffsetDateTime.now();
        return AuditLogEntity.builder()
                .auditUuid(UUID.randomUUID())
                .invoice(InvoiceEntity.builder().invoiceId(INVOICE_ID).build())
                .user(UserEntity.builder().userId(USER_ID).build())
                .actionType(ActionTypeEnum.SYSTEM_INGESTED)
                .actionTaken("File uploaded and stored")
                .fromState("NONE")
                .toState("INGESTED")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}