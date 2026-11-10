package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyVendorMappingEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.VendorEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvoiceAlreadyExistsException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.InvalidInvoiceException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.ResourceNotFoundException;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.*;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.InvoiceRepository;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository.PropertyVendorMappingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    // Still moving through the pipeline. APPROVED, PAID and VOID are past the processing queue.
    // The frontend's "All pending" tab (features/invoices/invoiceStatus.ts) uses the same list.
    static final List<String> PENDING_STATUSES = List.of(
        InvoiceStatus.INGESTED.getValue(),
        InvoiceStatus.PENDING_MAPPING.getValue(),
        InvoiceStatus.PENDING_APPROVAL.getValue(),
        InvoiceStatus.PROCESSING_PAYMENT.getValue(),
        InvoiceStatus.FAILED.getValue());

    private final InvoiceRepository invoiceRepository;
    private final PropertyVendorMappingRepository mappingRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PropertyVendorMappingRepository mappingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.mappingRepository = mappingRepository;
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse getInvoices(int page, int size, List<InvoiceStatus> statuses, String search) {
        Page<InvoiceEntity> invoicePage = invoiceRepository.findAll(
                matching(statuses, search),
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
    public InvoiceSummaryResponse getSummary(OffsetDateTime receivedSince) {
        String needsReview = InvoiceStatus.PENDING_MAPPING.getValue();
        String awaitingApproval = InvoiceStatus.PENDING_APPROVAL.getValue();

        InvoiceSummaryResponse summary = new InvoiceSummaryResponse();
        summary.setPendingCount(invoiceRepository.countByInvoiceStatusIn(PENDING_STATUSES));
        summary.setPendingPropertyCount(invoiceRepository.countDistinctPropertiesByInvoiceStatusIn(PENDING_STATUSES));
        summary.setNeedsReviewCount(invoiceRepository.countByInvoiceStatus(needsReview));
        summary.setNeedsReviewReceivedSince(receivedSince == null
                ? 0L
                : invoiceRepository.countByInvoiceStatusAndCreatedAtGreaterThanEqual(needsReview, receivedSince));
        summary.setAwaitingApprovalCount(invoiceRepository.countByInvoiceStatus(awaitingApproval));
        summary.setAwaitingApprovalTotal(invoiceRepository.sumAmountByInvoiceStatus(awaitingApproval));
        summary.setExceptionsCount(invoiceRepository.countByInvoiceStatus(InvoiceStatus.FAILED.getValue()));
        return summary;
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

    private static Specification<InvoiceEntity> matching(List<InvoiceStatus> statuses, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("invoiceStatus").in(statuses.stream().map(InvoiceStatus::getValue).toList()));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
                // LEFT joins so unmapped invoices (no vendor or property yet) can still match on filename
                Join<InvoiceEntity, PropertyVendorMappingEntity> mapping = root.join("mapping", JoinType.LEFT);
                Join<PropertyVendorMappingEntity, VendorEntity> vendor = mapping.join("vendor", JoinType.LEFT);
                Join<PropertyVendorMappingEntity, PropertyEntity> property = mapping.join("property", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("vendorSuppliedInvoiceId")), pattern, '\\'),
                        cb.like(cb.lower(root.get("originalFilename")), pattern, '\\'),
                        cb.like(cb.lower(vendor.get("name")), pattern, '\\'),
                        cb.like(cb.lower(property.get("name")), pattern, '\\')));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    // Treat % and _ typed by the user as literal characters, not wildcards
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    private InvoiceResponse toResponse(InvoiceEntity entity) {
        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceUuid(entity.getInvoiceId());
        PropertyVendorMappingEntity mapping = entity.getMapping();
        if (mapping != null) {
            response.setMappingUuid(mapping.getMappingId());
            response.setVendorAccountNumber(mapping.getVendorAccountNumber());
            if (mapping.getVendor() != null) {
                response.setVendorUuid(mapping.getVendor().getVendorId());
                response.setVendorName(mapping.getVendor().getName());
            }
            if (mapping.getProperty() != null) {
                response.setPropertyUuid(mapping.getProperty().getPropertyId());
                response.setPropertyName(mapping.getProperty().getName());
           }
        }
        response.setOriginalFilename(entity.getOriginalFilename());
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
