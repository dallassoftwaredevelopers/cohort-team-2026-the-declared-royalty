package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.BadRequestException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.GlobalExceptionHandler;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@Import(GlobalExceptionHandler.class)
class AuditLogControllerTest {

    private static final String BASE = "/api/v1/audit-logs";
    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuditLogService auditLogService;

    @Test
    void createAuditLog_returns201WithBody() throws Exception {
        AuditLogResponse response = sampleResponse();
        when(auditLogService.createAuditLog(any(AuditLogCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditLogId").value(response.getAuditLogId().toString()))
                .andExpect(jsonPath("$.invoice").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$.actionType").value("SYSTEM_INGESTED"));
    }

    @Test
    void createAuditLog_invoiceMissing_returns404ErrorResponse() throws Exception {
        when(auditLogService.createAuditLog(any(AuditLogCreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Invoice not found: " + INVOICE_ID));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(BASE));
    }

    @Test
    void getAuditLogById_found_returns200() throws Exception {
        AuditLogResponse response = sampleResponse();
        when(auditLogService.getAuditLogById(response.getAuditLogId())).thenReturn(response);

        mockMvc.perform(get(BASE + "/{id}", response.getAuditLogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditLogId").value(response.getAuditLogId().toString()));
    }

    @Test
    void getAuditLogById_notFound_returns404ErrorResponse() throws Exception {
        UUID missing = UUID.randomUUID();
        when(auditLogService.getAuditLogById(missing))
                .thenThrow(new ResourceNotFoundException("Audit log not found: " + missing));

        mockMvc.perform(get(BASE + "/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Audit log not found: " + missing));
    }

    @Test
    void getAuditLogs_passesQueryParamsToService() throws Exception {
        when(auditLogService.getAuditLogs(INVOICE_ID, "SYSTEM_PAID", null, null))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(BASE)
                        .param("invoice", INVOICE_ID.toString())
                        .param("actionType", "SYSTEM_PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(auditLogService).getAuditLogs(INVOICE_ID, "SYSTEM_PAID", null, null);
    }

    @Test
    void getAuditLogs_invalidActionType_returns400ErrorResponse() throws Exception {
        when(auditLogService.getAuditLogs(null, "BOGUS", null, null))
                .thenThrow(new BadRequestException("Invalid actionType: BOGUS"));

        mockMvc.perform(get(BASE).param("actionType", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid actionType: BOGUS"));
    }

    // ---------- helpers ----------

    private String validRequestJson() {
        return """
                {
                  "invoice": "%s",
                  "user": "%s",
                  "actionType": "SYSTEM_INGESTED",
                  "actionTaken": "File uploaded and stored",
                  "fromState": "NONE",
                  "toState": "INGESTED",
                  "notes": "sha256=abc"
                }
                """.formatted(INVOICE_ID, USER_ID);
    }

    private AuditLogResponse sampleResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        AuditLogResponse response = new AuditLogResponse();
        response.setAuditLogId(UUID.randomUUID());
        response.setInvoice(INVOICE_ID);
        response.setUser(USER_ID);
        response.setActionType("SYSTEM_INGESTED");
        response.setActionTaken("File uploaded and stored");
        response.setFromState("NONE");
        response.setToState("INGESTED");
        response.setNotes("sha256=abc");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        return response;
    }
}