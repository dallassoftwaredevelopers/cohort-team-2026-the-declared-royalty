package org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @Column(name = "audit_uuid", updatable = false, nullable = false)
    private UUID auditUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_uuid", nullable = false)
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", nullable = false)
    private UserEntity user;

    @Column(name = "action_type")
    @Enumerated(EnumType.STRING)
    private AuditLogCreateRequest.ActionTypeEnum actionType;

    @Column(name = "action_taken", length = 225)
    private String actionTaken;

    @Column(name = "from_state")
    private String fromState;

    @Column(name = "to_state")
    private String toState;

    @Column(name = "notes", length = 3000)
    private String notes;

    @Column(name = "created_at", insertable = false, nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, nullable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
