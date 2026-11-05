package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.BadRequestException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest.ActionTypeEnum;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.AuditLogRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class AuditLogService {

    private static final int ACTION_TAKEN_MAX = 225;
    private static final int STATE_MAX = 255;
    private static final int NOTES_MAX = 3000;

    private final AuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           InvoiceRepository invoiceRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
    }

    public AuditLogResponse createAuditLog(AuditLogCreateRequest request) {
        return record(
                request.getInvoice(),
                request.getUser(),
                request.getActionType(),
                request.getActionTaken(),
                request.getFromState(),
                request.getToState(),
                request.getNotes());
    }

    /**
     * Internal entry point for other services (e.g. InvoiceUploadService).
     * Joins the caller's transaction, so the log and the business change commit together.
     */
    public AuditLogResponse record(UUID invoiceId, UUID userId, ActionTypeEnum actionType,
                                   String actionTaken, String fromState, String toState, String notes) {
        if (invoiceId == null) throw new BadRequestException("invoice is required");
        if (userId == null) throw new BadRequestException("user is required");
        if (actionType == null) throw new BadRequestException("actionType is required");
        requireText(actionTaken, "actionTaken", ACTION_TAKEN_MAX);
        requireText(fromState, "fromState", STATE_MAX);
        requireText(toState, "toState", STATE_MAX);
        if (notes != null && notes.length() > NOTES_MAX) {
            throw new BadRequestException("notes must be at most " + NOTES_MAX + " characters");
        }

        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        }
        // TODO (OAuth): resolve the acting user from the security context instead of trusting the request
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        AuditLogEntity entity = AuditLogEntity.builder()
                .auditUuid(UUID.randomUUID())
                .invoice(invoiceRepository.getReferenceById(invoiceId))
                .user(userRepository.getReferenceById(userId))
                .actionType(actionType)
                .actionTaken(actionTaken)
                .fromState(fromState)
                .toState(toState)
                .notes(notes)
                .build();

        AuditLogEntity saved = auditLogRepository.save(entity);
        log.info("Audit log {} recorded: invoice={}, action={}, {} -> {}",
                saved.getAuditUuid(), invoiceId, actionType, fromState, toState);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(UUID invoice, String actionType, String fromState, String toState) {
        return auditLogRepository.findAuditLogsByFilters(
                        invoice, parseActionType(actionType), blankToNull(fromState), blankToNull(toState))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(UUID auditUuid) {
        return auditLogRepository.findById(auditUuid)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + auditUuid));
    }

    private ActionTypeEnum parseActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        try {
            return ActionTypeEnum.fromValue(actionType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid actionType: " + actionType);
        }
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new BadRequestException(field + " must be at most " + maxLength + " characters");
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
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