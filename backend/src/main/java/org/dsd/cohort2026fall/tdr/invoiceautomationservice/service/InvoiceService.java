package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyVendorMappingEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvoiceAlreadyExistsException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidInvoiceException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceCreateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoicePageResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceStatus;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.InvoiceUpdateRequest;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.PropertyVendorMappingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PropertyVendorMappingRepository mappingRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PropertyVendorMappingRepository mappingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.mappingRepository = mappingRepository;
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse getInvoices(int page, int size) {
        Page<InvoiceEntity> invoicePage = invoiceRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("invoiceId"))));

        InvoicePageResponse response = new InvoicePageResponse();
        response.setItems(invoicePage.getContent().stream().map(this::toResponse).toList());
        response.setPage(invoicePage.getNumber());
        response.setSize(invoicePage.getSize());
        response.setTotalElements(invoicePage.getTotalElements());
        response.setTotalPages(invoicePage.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceUuid) {
        return toResponse(findInvoice(invoiceUuid));
    }

    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        PropertyVendorMappingEntity mapping = findMapping(request.getMappingUuid());
        validateBillingPeriod(request.getBillingPeriodStart(), request.getBillingPeriodEnd());
        validateAmount(request.getAmount());
        ensureNotDuplicate(mapping.getMappingId(), request.getVendorSuppliedInvoiceId(), request.getBillingPeriodEnd(), null);

        OffsetDateTime now = now();
        InvoiceEntity invoice = InvoiceEntity.builder()
                .invoiceId(UUID.randomUUID())
                .vendorSuppliedInvoiceId(request.getVendorSuppliedInvoiceId())
                .mapping(mapping)
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .invoiceCreationDate(request.getInvoiceCreationDate())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .invoiceStatus(request.getInvoiceStatus() == null
                        ? InvoiceStatus.INGESTED.getValue()
                        : request.getInvoiceStatus().getValue())
                .blobFileUrl(request.getBlobFileUrl())
                .blobFileSha256(request.getBlobFileSha256())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse updateInvoice(UUID invoiceUuid, InvoiceUpdateRequest request) {
        InvoiceEntity invoice = findInvoice(invoiceUuid);
        PropertyVendorMappingEntity mapping = request.getMappingUuid() == null
                ? invoice.getMapping()
                : findMapping(request.getMappingUuid());

        var billingPeriodStart = request.getBillingPeriodStart() == null
                ? invoice.getBillingPeriodStart()
                : request.getBillingPeriodStart();
        var billingPeriodEnd = request.getBillingPeriodEnd() == null
                ? invoice.getBillingPeriodEnd()
                : request.getBillingPeriodEnd();
        validateBillingPeriod(billingPeriodStart, billingPeriodEnd);
        validateAmount(request.getAmount());
        ensureNotDuplicate(mapping.getMappingId(), invoice.getVendorSuppliedInvoiceId(), billingPeriodEnd, invoiceUuid);

        invoice.setMapping(mapping);
        if (request.getBillingPeriodStart() != null) {
            invoice.setBillingPeriodStart(request.getBillingPeriodStart());
        }
        if (request.getBillingPeriodEnd() != null) {
            invoice.setBillingPeriodEnd(request.getBillingPeriodEnd());
        }
        if (request.getInvoiceCreationDate() != null) {
            invoice.setInvoiceCreationDate(request.getInvoiceCreationDate());
        }
        if (request.getAmount() != null) {
            invoice.setAmount(request.getAmount());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getInvoiceStatus() != null) {
            invoice.setInvoiceStatus(request.getInvoiceStatus().getValue());
        }
        if (request.getFailureReason() != null) {
            invoice.setFailureReason(request.getFailureReason());
        }
        if (request.getBlobFileUrl() != null) {
            invoice.setBlobFileUrl(request.getBlobFileUrl());
        }
        if (request.getBlobFileSha256() != null) {
            invoice.setBlobFileSha256(request.getBlobFileSha256());
        }
        invoice.setUpdatedAt(now());

        return toResponse(invoiceRepository.save(invoice));
    }

    private InvoiceEntity findInvoice(UUID invoiceUuid) {
        return invoiceRepository.findById(invoiceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceUuid));
    }

    private PropertyVendorMappingEntity findMapping(UUID mappingUuid) {
        return mappingRepository.findById(mappingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Property-vendor mapping not found: " + mappingUuid));
    }

    private void ensureNotDuplicate(UUID mappingUuid, String vendorInvoiceId, java.time.LocalDate billingPeriodEnd, UUID currentInvoiceUuid) {
        if (vendorInvoiceId == null || billingPeriodEnd == null) {
            return;
        }

        boolean duplicate = currentInvoiceUuid == null
                ? invoiceRepository.existsByMapping_MappingIdAndVendorSuppliedInvoiceIdAndBillingPeriodEnd(
                mappingUuid, vendorInvoiceId, billingPeriodEnd)
                : invoiceRepository.existsByMapping_MappingIdAndVendorSuppliedInvoiceIdAndBillingPeriodEndAndInvoiceIdNot(
                mappingUuid, vendorInvoiceId, billingPeriodEnd, currentInvoiceUuid);
        if (duplicate) {
            throw new InvoiceAlreadyExistsException("An invoice with this vendor identifier and billing period already exists for the mapping.");
        }
    }

    private void validateBillingPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidInvoiceException("billingPeriodStart must not be after billingPeriodEnd.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount != null && (amount.abs().compareTo(new BigDecimal("99999999.99")) > 0
                || amount.stripTrailingZeros().scale() > 2)) {
            throw new InvalidInvoiceException("amount must fit DECIMAL(10, 2).");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    private InvoiceResponse toResponse(InvoiceEntity entity) {
        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceUuid(entity.getInvoiceId());
        response.setMappingUuid(entity.getMapping().getMappingId());
        response.setVendorSuppliedInvoiceId(entity.getVendorSuppliedInvoiceId());
        response.setBillingPeriodStart(entity.getBillingPeriodStart());
        response.setBillingPeriodEnd(entity.getBillingPeriodEnd());
        response.setInvoiceCreationDate(entity.getInvoiceCreationDate());
        response.setAmount(entity.getAmount());
        response.setDueDate(entity.getDueDate());
        response.setInvoiceStatus(InvoiceStatus.fromValue(entity.getInvoiceStatus()));
        response.setFailureReason(entity.getFailureReason());
        response.setBlobFileUrl(entity.getBlobFileUrl());
        response.setBlobFileSha256(entity.getBlobFileSha256());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
