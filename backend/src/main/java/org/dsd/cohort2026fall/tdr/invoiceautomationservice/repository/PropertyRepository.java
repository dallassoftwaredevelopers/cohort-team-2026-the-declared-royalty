package org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.PropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<PropertyEntity, UUID> {
}