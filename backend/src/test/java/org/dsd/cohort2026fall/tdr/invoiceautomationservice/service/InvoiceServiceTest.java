package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyVendorMappingEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.VendorEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvoiceAlreadyExistsException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidInvoiceException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoicePageResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceStatus;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceSummaryResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.PropertyVendorMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void getInvoices_returnsUnmappedUploadWithoutVendorOrProperty() {
        InvoiceEntity upload = new InvoiceEntity();
        upload.setInvoiceId(UUID.randomUUID());
        upload.setInvoiceStatus(InvoiceStatus.INGESTED.getValue());
        upload.setOriginalFilename("northline-bill.pdf");
        when(invoiceRepository.findAll(ArgumentMatchers.<Specification<InvoiceEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(upload), PageRequest.of(0, 20), 1));

        InvoicePageResponse page = invoiceService.getInvoices(0, 20, null, null);

        var item = page.getItems().get(0);
        assertNull(item.getMappingUuid());
        assertNull(item.getVendorName());
        assertNull(item.getPropertyName());
        assertEquals("northline-bill.pdf", item.getOriginalFilename());
    }

    @Test
    void getInvoices_includesVendorAndPropertyForMappedInvoice() {
        VendorEntity vendor = new VendorEntity();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setName("Northline Electric");
        PropertyEntity property = new PropertyEntity();
        property.setPropertyId(UUID.randomUUID());
        property.setName("Industrial Business Park");
        PropertyVendorMappingEntity mapping = new PropertyVendorMappingEntity();
        mapping.setMappingId(mappingUuid);
        mapping.setVendor(vendor);
        mapping.setProperty(property);
        mapping.setVendorAccountNumber("2000456700");

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setMapping(mapping);
        invoice.setInvoiceStatus(InvoiceStatus.PENDING_APPROVAL.getValue());
        when(invoiceRepository.findAll(ArgumentMatchers.<Specification<InvoiceEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice), PageRequest.of(0, 20), 1));

        var item = invoiceService.getInvoices(0, 20, List.of(InvoiceStatus.PENDING_APPROVAL), "north")
                 .getItems().get(0);

        assertEquals(mappingUuid, item.getMappingUuid());
        assertEquals("Northline Electric", item.getVendorName());
        assertEquals("2000456700", item.getVendorAccountNumber());
        assertEquals("Industrial Business Park", item.getPropertyName());
   }

   @Test
   void getSummary_countsEachDashboardGroup() {
       OffsetDateTime since = OffsetDateTime.parse("2026-11-09T06:00:00Z");
       when(invoiceRepository.countByInvoiceStatusIn(InvoiceService.PENDING_STATUSES)).thenReturn(24L);
       when(invoiceRepository.countDistinctPropertiesByInvoiceStatusIn(InvoiceService.PENDING_STATUSES)).thenReturn(8L);
       when(invoiceRepository.countByInvoiceStatus(anyString())).thenReturn(0L);
       when(invoiceRepository.countByInvoiceStatus("PENDING_MAPPING")).thenReturn(7L);
       when(invoiceRepository.countByInvoiceStatus("PENDING_APPROVAL")).thenReturn(5L);
       when(invoiceRepository.countByInvoiceStatus("FAILED")).thenReturn(2L);
       when(invoiceRepository.countByInvoiceStatusAndCreatedAtGreaterThanEqual("PENDING_MAPPING", since)).thenReturn(3L);
       when(invoiceRepository.sumAmountByInvoiceStatus("PENDING_APPROVAL")).thenReturn(new BigDecimal("18642.00"));

       InvoiceSummaryResponse summary = invoiceService.getSummary(since);

       assertEquals(24L, summary.getPendingCount());
       assertEquals(8L, summary.getPendingPropertyCount());
       assertEquals(7L, summary.getNeedsReviewCount());
       assertEquals(3L, summary.getNeedsReviewReceivedSince());
       assertEquals(5L, summary.getAwaitingApprovalCount());
       assertEquals(new BigDecimal("18642.00"), summary.getAwaitingApprovalTotal());
       assertEquals(2L, summary.getExceptionsCount());
   }

   @Test
   void getSummary_withoutReceivedSince_reportsZeroReceivedToday() {
       when(invoiceRepository.sumAmountByInvoiceStatus(anyString())).thenReturn(BigDecimal.ZERO);

       InvoiceSummaryResponse summary = invoiceService.getSummary(null);

       assertEquals(0L, summary.getNeedsReviewReceivedSince());
       verify(invoiceRepository, never()).countByInvoiceStatusAndCreatedAtGreaterThanEqual(anyString(), any());
    }
}