package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.GlobalExceptionHandler;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoicePageResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceStatus;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.InvoiceService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import(GlobalExceptionHandler.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void getInvoicesUsesDefaultPageAndSize() throws Exception {
        InvoiceResponse invoice = invoiceResponse(UUID.randomUUID(), UUID.randomUUID());
        InvoicePageResponse page = new InvoicePageResponse(List.of(invoice), 0, 20, 1L, 1);
        when(invoiceService.getInvoices(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].invoiceUuid").value(invoice.getInvoiceUuid().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getInvoicesRejectsSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/invoices").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void createInvoiceReturnsCreatedInvoice() throws Exception {
        UUID mappingUuid = UUID.randomUUID();
        InvoiceResponse invoice = invoiceResponse(UUID.randomUUID(), mappingUuid);
        when(invoiceService.createInvoice(any())).thenReturn(invoice);

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mappingUuid\":\"" + mappingUuid + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceUuid").value(invoice.getInvoiceUuid().toString()))
                .andExpect(jsonPath("$.mappingUuid").value(mappingUuid.toString()))
                .andExpect(jsonPath("$.invoiceStatus").value("INGESTED"));
    }

    @Test
    void patchInvoiceCallsUpdateServiceAndReturnsInvoice() throws Exception {
        UUID invoiceUuid = UUID.randomUUID();
        UUID mappingUuid = UUID.randomUUID();
        InvoiceResponse invoice = invoiceResponse(invoiceUuid, mappingUuid);
        when(invoiceService.updateInvoice(eq(invoiceUuid), any())).thenReturn(invoice);

        mockMvc.perform(patch("/api/v1/invoices/{invoiceUuid}", invoiceUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":12.34}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceUuid").value(invoiceUuid.toString()));

        verify(invoiceService).updateInvoice(eq(invoiceUuid), any());
    }

    private InvoiceResponse invoiceResponse(UUID invoiceUuid, UUID mappingUuid) {
        InvoiceResponse invoice = new InvoiceResponse();
        invoice.setInvoiceUuid(invoiceUuid);
        invoice.setMappingUuid(mappingUuid);
        invoice.setInvoiceStatus(InvoiceStatus.INGESTED);
        invoice.setCreatedAt(OffsetDateTime.parse("2026-11-04T12:00:00Z"));
        invoice.setUpdatedAt(OffsetDateTime.parse("2026-11-04T12:00:00Z"));
        return invoice;
    }
}
