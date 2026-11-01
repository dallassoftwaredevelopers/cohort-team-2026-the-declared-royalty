package org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.VendorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<VendorEntity, UUID>, JpaSpecificationExecutor<VendorEntity>, PagingAndSortingRepository<VendorEntity, UUID> {

    Optional<VendorEntity> findByNameIgnoreCase(String vendorName);

    //Optional<VendorEntity> findByVenderInvoiceNumber(String vendorInvoiceNumber);

    //List<VendorEntity> findByNameContainingIgnoreCase(String keyword);

    //Page<VendorEntity> paginateActiveVendors(String vendorStatus, Pageable pageable);
}
