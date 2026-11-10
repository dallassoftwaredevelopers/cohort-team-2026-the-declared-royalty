package org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, JpaSpecificationExecutor<InvoiceEntity> {
    Page<InvoiceEntity> findByMapping_Property_PropertyId(UUID propertyId, Pageable pageable);

    Page<InvoiceEntity> findByInvoiceStatus(String status, Pageable pageable);

    Optional<InvoiceEntity> findByMapping_Vendor_VendorIdAndVendorSuppliedInvoiceIdAndBillingPeriodEnd(
            UUID vendorId, String vendorSuppliedInvoiceId, LocalDate billingPeriodEnd);

    boolean existsByMapping_MappingIdAndVendorSuppliedInvoiceIdAndBillingPeriodEnd(
            UUID mappingUuid, String vendorInvoiceId, LocalDate billingPeriodEnd);

    boolean existsByMapping_MappingIdAndVendorSuppliedInvoiceIdAndBillingPeriodEndAndInvoiceIdNot(
            UUID mappingUuid, String vendorInvoiceId, LocalDate billingPeriodEnd, UUID currentInvoiceUuid);

    Optional<InvoiceEntity> findByBlobFileSha256(String blobFileSha256);

    boolean existsByBlobFileSha256(String blobFileSha256);

    @Query("select i.blobFileSha256 from InvoiceEntity i where i.blobFileSha256 in :hashes")
    List<String> findExistingHashes(@Param("hashes") Collection<String> hashes);

    long countByInvoiceStatus(String status);

    long countByInvoiceStatusIn(Collection<String> statuses);

    long countByInvoiceStatusAndCreatedAtGreaterThanEqual(String status, OffsetDateTime since);

    // Unmapped invoices have no property, so the inner join leaves them out
    @Query("select count(distinct p.propertyId) from InvoiceEntity i join i.mapping m join m.property p "
            + "where i.invoiceStatus in :statuses")

    long countDistinctPropertiesByInvoiceStatusIn(@Param("statuses") Collection<String> statuses);

    @Query("select coalesce(sum(i.amount), 0) from InvoiceEntity i where i.invoiceStatus = :status")
    BigDecimal sumAmountByInvoiceStatus(@Param("status") String status);
}