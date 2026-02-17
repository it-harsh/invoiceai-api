package com.invoiceai.repository;

import com.invoiceai.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByOrganizationId(UUID organizationId);
    Optional<Category> findByOrganizationIdAndName(UUID organizationId, String name);
}
