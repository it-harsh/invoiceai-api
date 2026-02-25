package com.invoiceai.service;

import com.invoiceai.dto.request.UpdateVendorRequest;
import com.invoiceai.dto.response.VendorResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.Category;
import com.invoiceai.model.Organization;
import com.invoiceai.model.Vendor;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.VendorRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<VendorResponse> getVendors(Pageable pageable) {
        UUID orgId = TenantContext.getCurrentOrgId();
        return vendorRepository.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Transactional
    public VendorResponse updateVendor(UUID vendorId, UpdateVendorRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(vendorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        if (request.getDefaultCategoryId() != null) {
            Category category = categoryRepository.findById(request.getDefaultCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            vendor.setDefaultCategory(category);
        }

        return toResponse(vendorRepository.save(vendor));
    }

    /**
     * Auto-upsert vendor from expense data. Called whenever an expense is created.
     */
    @Transactional
    public void upsertFromExpense(UUID orgId, String vendorName, BigDecimal amount, LocalDate date, Category category) {
        String normalized = vendorName.trim().toLowerCase();
        Vendor vendor = vendorRepository.findByOrganizationIdAndNormalizedName(orgId, normalized)
                .orElseGet(() -> Vendor.builder()
                        .organization(Organization.builder().id(orgId).build())
                        .name(vendorName.trim())
                        .normalizedName(normalized)
                        .expenseCount(0)
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        vendor.setExpenseCount(vendor.getExpenseCount() + 1);
        vendor.setTotalAmount(vendor.getTotalAmount().add(amount));
        vendor.setLastExpenseDate(date);

        // Set default category from first expense if not already set
        if (vendor.getDefaultCategory() == null && category != null) {
            vendor.setDefaultCategory(category);
        }

        vendorRepository.save(vendor);
    }

    /**
     * Looks up the default category for a vendor (used during bulk import).
     */
    @Transactional(readOnly = true)
    public Category getDefaultCategory(UUID orgId, String vendorName) {
        String normalized = vendorName.trim().toLowerCase();
        return vendorRepository.findByOrganizationIdAndNormalizedName(orgId, normalized)
                .map(Vendor::getDefaultCategory)
                .orElse(null);
    }

    private VendorResponse toResponse(Vendor vendor) {
        VendorResponse.VendorResponseBuilder builder = VendorResponse.builder()
                .id(vendor.getId())
                .name(vendor.getName())
                .expenseCount(vendor.getExpenseCount())
                .totalAmount(vendor.getTotalAmount())
                .lastExpenseDate(vendor.getLastExpenseDate());

        if (vendor.getDefaultCategory() != null) {
            builder.defaultCategory(VendorResponse.CategorySummary.builder()
                    .id(vendor.getDefaultCategory().getId())
                    .name(vendor.getDefaultCategory().getName())
                    .color(vendor.getDefaultCategory().getColor())
                    .build());
        }

        return builder.build();
    }
}
