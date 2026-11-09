package org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.PaymentMethod;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorResponse;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.VendorStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorEntity {

    @Id
    @Column(name = "vendor_uuid", updatable = false, nullable = false)
    private UUID vendorId;

    @Column(name = "vendor_name", nullable = false)
    private String name;

    @Column(name = "billing_phone_number")
    private String phoneNumber;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "payment_url")
    private String paymentUrl;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public VendorResponse toResponse() {
        VendorResponse response = new VendorResponse();
        response.setVendorUuid(getVendorId());
        response.setName(getName());
        response.setPhoneNumber(getPhoneNumber());
        response.setStatus(VendorStatus.fromValue(getStatus()));
        response.setPaymentUrl(getPaymentUrl());
        response.setPaymentMethod(PaymentMethod.fromValue(getPaymentMethod()));
        response.setCreatedAt(getCreatedAt());
        response.setUpdatedAt(getUpdatedAt());
        return response;
    }
}
