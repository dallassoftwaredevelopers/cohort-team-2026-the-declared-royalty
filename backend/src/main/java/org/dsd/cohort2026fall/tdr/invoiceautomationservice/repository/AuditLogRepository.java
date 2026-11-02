package org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.AuditLogEntity;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.model.AuditLogCreateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
    "(:invoice IS NULL OR a.invoice.invoiceId = :invoice) AND " +
    "(:actionType IS NULL OR a.actionType = :actionType) AND " +
    "(:fromState IS NULL OR a.fromState = :fromState) AND " +
    "(:toState IS NULL OR a.toState = :toState)")
    List<AuditLogEntity> findAuditLogsByFilters(
            @Param("invoice") UUID invoice,
            @Param("actionType") AuditLogCreateRequest.ActionTypeEnum actionType,
            @Param("fromState") String fromState,
            @Param("toState") String toState
    );
}
