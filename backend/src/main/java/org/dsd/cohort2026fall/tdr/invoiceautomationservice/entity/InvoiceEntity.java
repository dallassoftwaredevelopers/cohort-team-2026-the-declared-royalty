package org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity;


import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "vendor_supplied_invoice_id", updatable = false, nullable = false)
    private String vendorSuppliedInvoiceId;

    @ManyToOne
    @JoinColumn(name = "mapping_uuid" , nullable = false)
    private PropertyVendorMappingEntity mapping;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "invoice_creation_date", nullable = false)
    private LocalDate invoiceCreationDate;

    @Column(name = "amount", nullable = false)
    private float amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "invoice_status", nullable = false)
    private String invoiceStatus;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "blob_file_url", nullable = false)
    private String blobFileUrl;

    @Column(name = "blob_file_sha256", nullable = false)
    private String blobFileSha256;

    @Builder.Default
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

}
