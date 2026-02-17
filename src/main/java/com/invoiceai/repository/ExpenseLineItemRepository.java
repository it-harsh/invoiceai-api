package com.invoiceai.repository;

import com.invoiceai.model.ExpenseLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseLineItemRepository extends JpaRepository<ExpenseLineItem, UUID> {
    List<ExpenseLineItem> findByExpenseId(UUID expenseId);
}
