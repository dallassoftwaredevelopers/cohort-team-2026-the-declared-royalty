package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.api.AuditLogsApi;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.AuditLogRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.AuditLogService;
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

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(UUID invoice, String actionType, String fromState, String toState) {
        log.info("Searching audit logs. invoice={}, actionType={}, fromState={}, toState={}",
                invoice, actionType, fromState, toState);

        List<AuditLogResponse> logs = auditLogService.getAuditLogs(invoice, actionType, fromState, toState);
        log.info("Returning {} audit log records.", logs.size());
        return ResponseEntity.ok(logs);
    }

    @Override
    public ResponseEntity<AuditLogResponse> createAuditLog(AuditLogCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditLogService.createAuditLog(request));
    }

    @Override
    public ResponseEntity<AuditLogResponse> getAuditLogById(UUID auditUuid) {
        return ResponseEntity.ok(auditLogService.getAuditLogById(auditUuid));
    }
}
