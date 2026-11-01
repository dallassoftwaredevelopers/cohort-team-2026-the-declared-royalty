package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.VendorEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.GlobalExceptionHandler;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendorController.class)
@Import(GlobalExceptionHandler.class)
class VendorControllerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorRepository vendorRepository;

    private VendorEntity existingVendor(UUID id) {
        return VendorEntity.builder()
                .vendorId(id)
                .name("Acme Plumbing")
                .phoneNumber("555-123-4567")
                .status("ACTIVE")
                .paymentUrl("https://pay.acme.com")
                .paymentMethod("CHECK")
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }

    @Test
    void createVendor_appliesDefaultsWhenOmitted() throws Exception {
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme Plumbing\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorUuid").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Acme Plumbing"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.paymentMethod").value("ACH"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createVendor_keepsProvidedValues() throws Exception {
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"status\":\"INACTIVE\",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));
    }

    @Test
    void createVendor_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(vendorRepository, never()).save(any());
    }

    @Test
    void createVendor_invalidEnum_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getVendors_returnsList() throws Exception {
        when(vendorRepository.findAll()).thenReturn(List.of(existingVendor(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getVendors_returnsEmptyArray() throws Exception {
        when(vendorRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getVendorByUuid_found() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorRepository.findById(id)).thenReturn(Optional.of(existingVendor(id)));

        mockMvc.perform(get("/api/v1/vendors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorUuid").value(id.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("CHECK"));
    }

    @Test
    void getVendorByUuid_missing_returns404ErrorResponse() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/vendors/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Vendor not found: " + id))
                .andExpect(jsonPath("$.path").value("/api/v1/vendors/" + id))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void updateVendor_replacesFieldsAndKeepsIdentity() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorRepository.findById(id)).thenReturn(Optional.of(existingVendor(id)));
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/vendors/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme Renamed\",\"status\":\"INACTIVE\",\"paymentMethod\":\"ACH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorUuid").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Acme Renamed"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.paymentMethod").value("ACH"))
                // omitted optional fields are cleared (full replacement)
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.paymentUrl").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value(not("2026-01-01T00:00:00Z")));
    }

    @Test
    void updateVendor_missingRequiredField_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/vendors/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\"}"))
                .andExpect(status().isBadRequest());
        verify(vendorRepository, never()).save(any());
    }

    @Test
    void updateVendor_missing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/vendors/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"status\":\"ACTIVE\",\"paymentMethod\":\"ACH\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
