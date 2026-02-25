package com.invoiceai.service;

import com.invoiceai.model.*;
import com.invoiceai.model.enums.ExpenseStatus;
import com.invoiceai.model.enums.InvoiceStatus;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.repository.InvoiceRepository;
import com.invoiceai.service.ai.AiExtractionService;
import com.invoiceai.service.ai.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceProcessingService {

    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final AiExtractionService aiExtractionService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final VendorService vendorService;

    @Async
    @Transactional
    public void processInvoiceAsync(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.warn("Invoice {} not found for processing", invoiceId);
            return;
        }

        try {
            invoice.setStatus(InvoiceStatus.PROCESSING);
            invoice.setProcessingStartedAt(Instant.now());
            invoiceRepository.save(invoice);

            // Download file from R2
            byte[] fileBytes = fileStorageService.downloadFile(invoice.getFileKey());

            // Run AI extraction
            ExtractionResult result = aiExtractionService.extract(
                    fileBytes, invoice.getFileType(), invoice.getFileName());

            // Save raw AI response
            invoice.setAiRawResponse(result.getRawResponse());

            // Match category
            Category category = matchCategory(invoice.getOrganization().getId(), result.getCategoryHint());

            // Create expense from extraction result
            Expense expense = Expense.builder()
                    .organization(invoice.getOrganization())
                    .invoice(invoice)
                    .category(category)
                    .vendorName(result.getVendorName() != null ? result.getVendorName() : "Unknown Vendor")
                    .amount(result.getAmount() != null ? result.getAmount() : BigDecimal.ZERO)
                    .currency(result.getCurrency() != null ? result.getCurrency() : "USD")
                    .taxAmount(result.getTaxAmount() != null ? result.getTaxAmount() : BigDecimal.ZERO)
                    .date(result.getDate() != null ? result.getDate() : LocalDate.now())
                    .description(result.getDescription())
                    .status(ExpenseStatus.NEEDS_REVIEW)
                    .aiConfidence(result.getConfidence())
                    .build();

            // Add line items
            if (result.getLineItems() != null && !result.getLineItems().isEmpty()) {
                List<ExpenseLineItem> lineItems = new ArrayList<>();
                for (ExtractionResult.LineItem li : result.getLineItems()) {
                    ExpenseLineItem item = ExpenseLineItem.builder()
                            .expense(expense)
                            .description(li.getDescription() != null ? li.getDescription() : "Item")
                            .quantity(li.getQuantity() != null ? li.getQuantity() : BigDecimal.ONE)
                            .unitPrice(li.getUnitPrice() != null ? li.getUnitPrice() : BigDecimal.ZERO)
                            .total(li.getTotal() != null ? li.getTotal() : BigDecimal.ZERO)
                            .build();
                    lineItems.add(item);
                }
                expense.setLineItems(lineItems);
            }

            // Duplicate detection — flag only
            Expense duplicate = duplicateDetectionService.checkForDuplicate(
                    invoice.getOrganization().getId(),
                    expense.getVendorName(), expense.getAmount(), expense.getDate());
            if (duplicate != null) {
                expense.setDuplicate(true);
                expense.setDuplicateOf(duplicate);
            }

            expenseRepository.save(expense);

            // Auto-maintain vendor directory
            vendorService.upsertFromExpense(
                    invoice.getOrganization().getId(),
                    expense.getVendorName(), expense.getAmount(), expense.getDate(), category);

            invoice.setStatus(InvoiceStatus.EXTRACTED);
            invoice.setProcessingCompletedAt(Instant.now());
            invoiceRepository.save(invoice);

            log.info("Successfully processed invoice {} → expense {} (confidence: {})",
                    invoiceId, expense.getId(), result.getConfidence());

        } catch (Exception e) {
            log.error("Failed to process invoice {}", invoiceId, e);
            invoice.setStatus(InvoiceStatus.FAILED);
            invoice.setErrorMessage(e.getMessage());
            invoice.setProcessingCompletedAt(Instant.now());
            invoiceRepository.save(invoice);
        }
    }

    private Category matchCategory(UUID orgId, String categoryHint) {
        if (categoryHint == null || categoryHint.isBlank()) {
            return categoryRepository.findByOrganizationIdAndName(orgId, "Other").orElse(null);
        }

        return categoryRepository.findByOrganizationIdAndName(orgId, categoryHint)
                .orElseGet(() -> categoryRepository.findByOrganizationIdAndName(orgId, "Other").orElse(null));
    }
}
