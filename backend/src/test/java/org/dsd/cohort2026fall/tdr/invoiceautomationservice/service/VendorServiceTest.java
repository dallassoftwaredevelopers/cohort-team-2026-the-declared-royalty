package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.VendorEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PaymentMethod;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorStatus;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorUpdateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.VendorRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    private static final UUID VENDOR_ID = UUID.randomUUID();
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorService vendorService;

    @Test
    void getVendors_mapsEntitiesToResponses() {
        VendorEntity entity = existingVendor();
        when(vendorRepository.findAll()).thenReturn(List.of(entity));

        var response = vendorService.getVendors();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getVendorUuid()).isEqualTo(VENDOR_ID);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Acme Plumbing");
    }

    @Test
    void getVendorByUuid_found_returnsResponse() {
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.of(existingVendor()));

        var response = vendorService.getVendorByUuid(VENDOR_ID);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getVendorUuid()).isEqualTo(VENDOR_ID);
        assertThat(response.getBody().getPaymentMethod()).isEqualTo(PaymentMethod.CHECK);
    }

    @Test
    void getVendorByUuid_missing_throwsResourceNotFound() {
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.getVendorByUuid(VENDOR_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vendor not found: " + VENDOR_ID);
    }

    @Test
    void createVendor_appliesDefaultsAndStoresMissingOptionalValues() {
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VendorCreateRequest request = new VendorCreateRequest("Acme Plumbing");

        var response = vendorService.createVendor(request);

        ArgumentCaptor<VendorEntity> captor = ArgumentCaptor.forClass(VendorEntity.class);
        verify(vendorRepository).save(captor.capture());
        VendorEntity saved = captor.getValue();

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(saved.getVendorId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Acme Plumbing");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getPaymentMethod()).isEqualTo("ACH");
        assertThat(saved.getPhoneNumber()).isEqualTo("NULL");
        assertThat(saved.getPaymentUrl()).isEqualTo("NULL");
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
        assertThat(saved.getCreatedAt().getNano() % 1_000).isZero();
        assertThat(response.getBody().getVendorUuid()).isEqualTo(saved.getVendorId());
    }

    @Test
    void createVendor_mapsProvidedValuesAndNullKeyword() {
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VendorCreateRequest request = new VendorCreateRequest("Acme Plumbing");
        request.setPhoneNumber("555-123-4567");
        request.setPaymentUrl("NULL");
        request.setStatus(VendorStatus.INACTIVE);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        vendorService.createVendor(request);

        ArgumentCaptor<VendorEntity> captor = ArgumentCaptor.forClass(VendorEntity.class);
        verify(vendorRepository).save(captor.capture());
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("555-123-4567");
        assertThat(captor.getValue().getPaymentUrl()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo("INACTIVE");
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("CREDIT_CARD");
    }

    @Test
    void updateVendor_updatesProvidedFieldsAndKeepsIdentityAndCreationTime() {
        VendorEntity entity = existingVendor();
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.of(entity));
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VendorUpdateRequest request = new VendorUpdateRequest();
        request.setName("Acme Renamed");
        request.setStatus(VendorStatus.INACTIVE);

        var response = vendorService.updateVendor(VENDOR_ID, request);

        ArgumentCaptor<VendorEntity> captor = ArgumentCaptor.forClass(VendorEntity.class);
        verify(vendorRepository).save(captor.capture());
        VendorEntity saved = captor.getValue();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(saved.getVendorId()).isEqualTo(VENDOR_ID);
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isAfter(CREATED_AT);
        assertThat(saved.getName()).isEqualTo("Acme Renamed");
        assertThat(saved.getStatus()).isEqualTo("INACTIVE");
        assertThat(saved.getPhoneNumber()).isEqualTo("555-123-4567");
        assertThat(saved.getPaymentUrl()).isEqualTo("https://pay.acme.com");
        assertThat(saved.getPaymentMethod()).isEqualTo("CHECK");
    }

    @Test
    void updateVendor_nullKeywordClearsNullableFieldsAndEmptyStringLeavesThemUnchanged() {
        VendorEntity entity = existingVendor();
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.of(entity));
        when(vendorRepository.save(any(VendorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VendorUpdateRequest request = new VendorUpdateRequest();
        request.setPhoneNumber("Null");
        request.setPaymentUrl("");

        var response = vendorService.updateVendor(VENDOR_ID, request);

        assertThat(response.getBody().getPhoneNumber()).isNull();
        assertThat(response.getBody().getPaymentUrl()).isEqualTo("https://pay.acme.com");
        verify(vendorRepository).save(entity);
    }

    @Test
    void updateVendor_missing_throwsAndDoesNotSave() {
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.updateVendor(VENDOR_ID, new VendorUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vendor not found: " + VENDOR_ID);

        verify(vendorRepository, never()).save(any(VendorEntity.class));
    }

    private VendorEntity existingVendor() {
        return VendorEntity.builder()
                .vendorId(VENDOR_ID)
                .name("Acme Plumbing")
                .phoneNumber("555-123-4567")
                .status("ACTIVE")
                .paymentUrl("https://pay.acme.com")
                .paymentMethod("CHECK")
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }
}
