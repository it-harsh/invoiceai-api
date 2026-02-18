package com.invoiceai.repository;

import com.invoiceai.model.Expense;
import com.invoiceai.model.enums.ExpenseStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ExpenseSpecification {

    private ExpenseSpecification() {}

    public static Specification<Expense> withFilters(
            UUID orgId,
            ExpenseStatus status,
            UUID categoryId,
            String vendorName,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal amountMin,
            BigDecimal amountMax,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Eagerly fetch category to avoid LazyInitializationException (e.g. CSV export)
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("category", jakarta.persistence.criteria.JoinType.LEFT);
            }

            predicates.add(cb.equal(root.get("organization").get("id"), orgId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (vendorName != null && !vendorName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vendorName")), "%" + vendorName.toLowerCase() + "%"));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), dateTo));
            }
            if (amountMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), amountMin));
            }
            if (amountMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), amountMax));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("vendorName")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
