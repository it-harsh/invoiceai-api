package com.invoiceai.repository;

import com.invoiceai.model.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<Vendor> findByOrganizationIdAndNormalizedName(UUID organizationId, String normalizedName);
    Page<Vendor> findByOrganizationId(UUID organizationId, Pageable pageable);
    Optional<Vendor> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
