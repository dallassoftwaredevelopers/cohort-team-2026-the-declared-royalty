package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyVendorMappingEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvoiceAlreadyExistsException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidInvoiceException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceStatus;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.PropertyVendorMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final PropertyVendorMappingRepository mappingRepository = mock(PropertyVendorMappingRepository.class);
    private final InvoiceService invoiceService = new InvoiceService(invoiceRepository, mappingRepository);
    private final UUID mappingUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PropertyVendorMappingEntity mapping = new PropertyVendorMappingEntity();
        mapping.setMappingId(mappingUuid);
        when(mappingRepository.findById(mappingUuid)).thenReturn(Optional.of(mapping));
        when(invoiceRepository.existsByMapping_MappingIdAndVendorSuppliedInvoiceIdAndBillingPeriodEnd(
                mappingUuid, "INV-100", LocalDate.of(2026, 10, 31))).thenReturn(false);
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createInvoice_defaultsStatusAndPreservesExactAmount() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(mappingUuid);
        request.setVendorSuppliedInvoiceId("INV-100");
        request.setBillingPeriodStart(LocalDate.of(2026, 10, 1));
        request.setBillingPeriodEnd(LocalDate.of(2026, 10, 31));
        request.setAmount(new BigDecimal("1250.40"));

        var response = invoiceService.createInvoice(request);

        assertEquals(InvoiceStatus.INGESTED, response.getInvoiceStatus());
        assertEquals(new BigDecimal("1250.40"), response.getAmount());
        assertEquals(mappingUuid, response.getMappingUuid());
        verify(invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void createInvoice_rejectsDuplicateForSameMappingAndPeriod() {
        when(invoiceRepository.existsByMapping_MappingIdAndVendorSuppliedInvoiceIdAndBillingPeriodEnd(
                mappingUuid, "INV-100", LocalDate.of(2026, 10, 31))).thenReturn(true);
        InvoiceCreateRequest request = new InvoiceCreateRequest(mappingUuid);
        request.setVendorSuppliedInvoiceId("INV-100");
        request.setBillingPeriodEnd(LocalDate.of(2026, 10, 31));

        assertThrows(InvoiceAlreadyExistsException.class, () -> invoiceService.createInvoice(request));
        verify(invoiceRepository, never()).save(any(InvoiceEntity.class));
    }

    @Test
    void createInvoice_rejectsInvalidBillingPeriod() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(mappingUuid);
        request.setBillingPeriodStart(LocalDate.of(2026, 11, 1));
        request.setBillingPeriodEnd(LocalDate.of(2026, 10, 31));

        assertThrows(InvalidInvoiceException.class, () -> invoiceService.createInvoice(request));
        verify(invoiceRepository, never()).save(any(InvoiceEntity.class));
    }

    @Test
    void createInvoice_rejectsAmountWithMoreThanTwoDecimalPlaces() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(mappingUuid);
        request.setAmount(new BigDecimal("10.001"));

        assertThrows(InvalidInvoiceException.class, () -> invoiceService.createInvoice(request));
        verify(invoiceRepository, never()).save(any(InvoiceEntity.class));
    }
}