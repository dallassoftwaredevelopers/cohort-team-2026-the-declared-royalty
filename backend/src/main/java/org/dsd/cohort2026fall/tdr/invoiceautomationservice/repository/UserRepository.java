package org.dsd.cohort2026fall.tdr.invoiceautomationservice.repository;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmail(String email);
    Boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.properties WHERE u.status = :active")
    Page<UserEntity> findByActiveWithRelations(@Param("active") boolean active, Pageable pageable);

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.properties WHERE u.userId = :id")
    Optional<UserEntity> findByIdWithRelations(@Param("id") UUID id);

}
