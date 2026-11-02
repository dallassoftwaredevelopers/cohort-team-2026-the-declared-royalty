package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.AuditLogsApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.AuditLogRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class AuditLogController implements AuditLogsApi {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(UUID invoice, String actionType, String fromState, String toState) {
        log.info("REST Request received to search audit logs. Filters - Invoice: {}, Action: {}, ToState: {}",
                invoice, actionType, toState);

        AuditLogCreateRequest.ActionTypeEnum actionTypeEnum = null;
        if (actionType != null && !actionType.isBlank()) {
            actionTypeEnum = AuditLogCreateRequest.ActionTypeEnum.fromValue(actionType);
        }

        List<AuditLogResponse> logs = auditLogRepository.findAuditLogsByFilters(invoice, actionTypeEnum, fromState, toState).stream()
                .map(this::toResponse)
                .toList();

        log.info("Successfully returned {} audit log records.", logs.size());
        return ResponseEntity.ok(logs);
    }

    @Override
    public ResponseEntity<AuditLogResponse> createAuditLog(AuditLogCreateRequest request) {
        AuditLogEntity entity = new AuditLogEntity();

        entity.setAuditUuid(UUID.randomUUID());
        InvoiceEntity invoiceProxy = new InvoiceEntity();
        invoiceProxy.setInvoiceId(request.getInvoice());
        entity.setInvoice(invoiceProxy);

        UserEntity userProxy = UserEntity.builder()
                .userId(request.getUser())
                .build();
        entity.setUser(userProxy);

        entity.setActionType(request.getActionType());
        entity.setActionTaken(request.getActionTaken());
        entity.setFromState(request.getFromState());
        entity.setToState(request.getToState());
        entity.setNotes(request.getNotes());

        AuditLogEntity saved = auditLogRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @Override
    public ResponseEntity<AuditLogResponse> getAuditLogById(UUID auditUuid) {
        AuditLogEntity entity = auditLogRepository.findById(auditUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log entry not found"));
        return ResponseEntity.ok(toResponse(entity));
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        AuditLogResponse response = new AuditLogResponse();
        response.setAuditLogId(entity.getAuditUuid());

        if (entity.getInvoice() != null) {
            response.setInvoice(entity.getInvoice().getInvoiceId());
        }
        if (entity.getUser() != null) {
            response.setUser(entity.getUser().getUserId());
        }

        if (entity.getActionType() != null) {
            response.setActionType(entity.getActionType().getValue());
        }

        response.setActionTaken(entity.getActionTaken());
        response.setFromState(entity.getFromState());
        response.setToState(entity.getToState());
        response.setNotes(entity.getNotes());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
