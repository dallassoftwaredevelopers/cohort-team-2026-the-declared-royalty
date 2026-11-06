package org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    @Column(name = "invoice_uuid", updatable = false, nullable = false)
    private UUID invoiceId;

    @Column(name = "vendor_supplied_invoice_id")
    private String vendorSuppliedInvoiceId;

    @ManyToOne
    @JoinColumn(name = "mapping_uuid")
    private PropertyVendorMappingEntity mapping;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(name = "invoice_creation_date")
    private LocalDate invoiceCreationDate;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "invoice_status", nullable = false)
    private String invoiceStatus;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "blob_file_url")
    private String blobFileUrl;

    @Column(name = "blob_file_sha256", unique = true)
    private String blobFileSha256;

    @Column(name = "original_filename")
    private String originalFilename;

    @Builder.Default
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

}
