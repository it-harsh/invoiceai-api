package com.invoiceai.service;

import com.invoiceai.dto.request.CreateCategoryRequest;
import com.invoiceai.dto.request.UpdateCategoryRequest;
import com.invoiceai.dto.response.CategoryResponse;
import com.invoiceai.exception.BadRequestException;
import com.invoiceai.exception.DuplicateResourceException;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.Category;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.security.TenantContext;
import com.invoiceai.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        UUID orgId = TenantContext.getCurrentOrgId();
        return categoryRepository.findByOrganizationId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        categoryRepository.findByOrganizationIdAndName(orgId, request.getName().trim())
                .ifPresent(c -> { throw new DuplicateResourceException("Category already exists: " + request.getName()); });

        Category category = Category.builder()
                .organization(Organization.builder().id(orgId).build())
                .name(request.getName().trim())
                .color(request.getColor() != null ? request.getColor() : "#6B7280")
                .icon(request.getIcon())
                .isDefault(false)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Category category = categoryRepository.findById(categoryId)
                .filter(c -> c.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (request.getName() != null) {
            categoryRepository.findByOrganizationIdAndName(orgId, request.getName().trim())
                    .filter(c -> !c.getId().equals(categoryId))
                    .ifPresent(c -> { throw new DuplicateResourceException("Category already exists: " + request.getName()); });
            category.setName(request.getName().trim());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Category category = categoryRepository.findById(categoryId)
                .filter(c -> c.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isDefault()) {
            throw new BadRequestException("Cannot delete a default category");
        }

        // Check if category has expenses
        expenseRepository.findAll().stream()
                .filter(e -> e.getCategory() != null && e.getCategory().getId().equals(categoryId))
                .findFirst()
                .ifPresent(e -> { throw new BadRequestException("Category has associated expenses and cannot be deleted"); });

        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .icon(category.getIcon())
                .isDefault(category.isDefault())
                .build();
    }
}
