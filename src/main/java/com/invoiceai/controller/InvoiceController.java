package com.invoiceai.controller;

import com.invoiceai.dto.request.CreateInvoiceRequest;
import com.invoiceai.dto.request.UploadUrlRequest;
import com.invoiceai.dto.response.InvoiceResponse;
import com.invoiceai.dto.response.UploadUrlResponse;
import com.invoiceai.model.User;
import com.invoiceai.model.enums.InvoiceStatus;
import com.invoiceai.security.UserPrincipal;
import com.invoiceai.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(@Valid @RequestBody UploadUrlRequest request) {
        return ResponseEntity.ok(invoiceService.generateUploadUrl(request));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createInvoice(request, principal.getUser()));
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoices(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID id) {
        String url = invoiceService.getDownloadUrl(id);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }
}
