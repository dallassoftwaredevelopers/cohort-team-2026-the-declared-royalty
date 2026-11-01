package org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<PropertyEntity, UUID>, JpaSpecificationExecutor<PropertyEntity> {
    Page<PropertyEntity> findByPropertyId(UUID propertyId, Pageable pageable);
    //    Page<PropertyEntity> findByUserId(UUID userId, Pageable pageable);
    Boolean existsByStreetAddressIgnoreCaseAndZipCode(String streetAddress, String zipCode);
}