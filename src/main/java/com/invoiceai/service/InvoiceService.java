package com.invoiceai.service;

import com.invoiceai.dto.request.CreateInvoiceRequest;
import com.invoiceai.dto.request.UploadUrlRequest;
import com.invoiceai.dto.response.InvoiceResponse;
import com.invoiceai.dto.response.UploadUrlResponse;
import com.invoiceai.exception.BadRequestException;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.Expense;
import com.invoiceai.model.Invoice;
import com.invoiceai.model.Organization;
import com.invoiceai.model.User;
import com.invoiceai.model.enums.InvoiceStatus;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.repository.InvoiceRepository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final FileStorageService fileStorageService;
    private final InvoiceProcessingService invoiceProcessingService;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/webp");
    private static final long MAX_FILE_SIZE_FREE = 10 * 1024 * 1024; // 10MB

    public UploadUrlResponse generateUploadUrl(UploadUrlRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        if (!ALLOWED_TYPES.contains(request.getFileType())) {
            throw new BadRequestException("File type not supported. Allowed: PDF, PNG, JPEG, WebP");
        }

        if (request.getFileSize() > MAX_FILE_SIZE_FREE) {
            throw new BadRequestException("File too large. Maximum 10MB");
        }

        UUID invoiceId = UUID.randomUUID();
        String fileKey = fileStorageService.generateFileKey(orgId, invoiceId, request.getFileName());
        String uploadUrl = fileStorageService.generatePresignedUploadUrl(
                fileKey, request.getFileType(), request.getFileSize());

        return UploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .fileKey(fileKey)
                .build();
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, User user) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Invoice invoice = Invoice.builder()
                .organization(Organization.builder().id(orgId).build())
                .fileKey(request.getFileKey())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(request.getFileType())
                .status(InvoiceStatus.PROCESSING)
                .uploadedBy(user)
                .build();

        invoice = invoiceRepository.save(invoice);

        // Trigger async AI processing AFTER transaction commits
        UUID invoiceId = invoice.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invoiceProcessingService.processInvoiceAsync(invoiceId);
            }
        });

        return toResponse(invoice, null);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoices(InvoiceStatus status, Pageable pageable) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Page<Invoice> page;
        if (status != null) {
            page = invoiceRepository.findByOrganizationIdAndStatus(orgId, status, pageable);
        } else {
            page = invoiceRepository.findByOrganizationId(orgId, pageable);
        }

        return page.map(invoice -> {
            Expense expense = expenseRepository.findByInvoiceId(invoice.getId()).orElse(null);
            return toResponse(invoice, expense);
        });
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceId) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        Expense expense = expenseRepository.findByInvoiceId(invoiceId).orElse(null);
        return toResponse(invoice, expense);
    }

    public String getDownloadUrl(UUID invoiceId) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return fileStorageService.generatePresignedDownloadUrl(invoice.getFileKey());
    }

    private InvoiceResponse toResponse(Invoice invoice, Expense expense) {
        InvoiceResponse.InvoiceResponseBuilder builder = InvoiceResponse.builder()
                .id(invoice.getId())
                .fileName(invoice.getFileName())
                .fileSize(invoice.getFileSize())
                .fileType(invoice.getFileType())
                .status(invoice.getStatus().name())
                .errorMessage(invoice.getErrorMessage())
                .createdAt(invoice.getCreatedAt());

        if (invoice.getUploadedBy() != null) {
            builder.uploadedBy(InvoiceResponse.UserSummary.builder()
                    .id(invoice.getUploadedBy().getId())
                    .fullName(invoice.getUploadedBy().getFullName())
                    .build());
        }

        if (expense != null) {
            builder.expense(InvoiceResponse.ExpenseSummary.builder()
                    .id(expense.getId())
                    .vendorName(expense.getVendorName())
                    .amount(expense.getAmount())
                    .build());
        }

        return builder.build();
    }
}
