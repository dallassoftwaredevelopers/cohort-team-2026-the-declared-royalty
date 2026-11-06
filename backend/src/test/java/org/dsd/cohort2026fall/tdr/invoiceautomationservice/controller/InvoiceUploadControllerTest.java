package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.DuplicateFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.GlobalExceptionHandler;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidFileException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.StorageException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.InvoiceUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceUploadController.class)
@Import(GlobalExceptionHandler.class)
class InvoiceUploadControllerTest {

    private static final String UPLOAD_URL = "/api/v1/invoices/upload";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-01T00:00:00Z");
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceUploadService invoiceUploadService;

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "brightwater.pdf", "application/pdf",
                "%PDF-1.4\n%test\n".getBytes());
    }

    @Test
    void upload_success_returns201WithInvoiceDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(invoiceUploadService.upload(any())).thenReturn(InvoiceEntity.builder()
                .invoiceId(id)
                .invoiceStatus("INGESTED")
                .originalFilename("brightwater.pdf")
                .createdAt(CREATED_AT)
                .build());

        mockMvc.perform(multipart(UPLOAD_URL).file(pdf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("INGESTED"))
                .andExpect(jsonPath("$.originalFilename").value("brightwater.pdf"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void upload_invalidFile_returns400ErrorResponse() throws Exception {
        when(invoiceUploadService.upload(any()))
                .thenThrow(new InvalidFileException("File type invalid: only PDF, PNG and JPEG are accepted"));

        mockMvc.perform(multipart(UPLOAD_URL).file(pdf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("File type invalid: only PDF, PNG and JPEG are accepted"))
                .andExpect(jsonPath("$.path").value(UPLOAD_URL))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void upload_duplicate_returns409ErrorResponse() throws Exception {
        when(invoiceUploadService.upload(any()))
                .thenThrow(new DuplicateFileException("Duplicate file: this file has already been uploaded"));

        mockMvc.perform(multipart(UPLOAD_URL).file(pdf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Duplicate file: this file has already been uploaded"));
    }

    @Test
    void upload_storageDown_returns503WithoutLeakingInternals() throws Exception {
        when(invoiceUploadService.upload(any()))
                .thenThrow(new StorageException("Failed to store file in MinIO",
                        new RuntimeException("InvalidAccessKeyId")));

        mockMvc.perform(multipart(UPLOAD_URL).file(pdf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("File storage is unavailable, try again later"));
    }

    @Test
    void upload_tooLarge_returns413ErrorResponse() throws Exception {
        // In production the multipart resolver throws this before the controller runs;
        // here it's thrown from the service just to prove the handler maps it to 413
        when(invoiceUploadService.upload(any()))
                .thenThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024));

        mockMvc.perform(multipart(UPLOAD_URL).file(pdf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.message").value("File too large: maximum size is 5MB"));
    }

    @Test
    void upload_wrongFieldName_returns400AndNeverCallsService() throws Exception {
        // Frontend must send the file under the "file" field
        MockMultipartFile wrongField = new MockMultipartFile("document", "bill.pdf",
                "application/pdf", "%PDF-1.4\n".getBytes());

        mockMvc.perform(multipart(UPLOAD_URL).file(wrongField))
                .andExpect(status().isBadRequest());

        verify(invoiceUploadService, never()).upload(any());
    }

    @Test
    void checkDuplicates_returnsMatchingHashes() throws Exception {
        when(invoiceUploadService.findExistingHashes(any())).thenReturn(List.of(HASH_A));

        mockMvc.perform(post(UPLOAD_URL + "/check-duplicates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashes\":[\"" + HASH_A + "\",\"" + HASH_B + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicates", hasSize(1)))
                .andExpect(jsonPath("$.duplicates[0]").value(HASH_A));
    }

    @Test
    void checkDuplicates_malformedHash_returns400() throws Exception {
        mockMvc.perform(post(UPLOAD_URL + "/check-duplicates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashes\":[\"not-a-hash\"]}"))
                .andExpect(status().isBadRequest());

        verify(invoiceUploadService, never()).findExistingHashes(any());
    }
}