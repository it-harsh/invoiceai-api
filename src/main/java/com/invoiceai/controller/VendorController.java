package com.invoiceai.controller;

import com.invoiceai.dto.request.UpdateVendorRequest;
import com.invoiceai.dto.response.VendorResponse;
import com.invoiceai.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<Page<VendorResponse>> getVendors(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(vendorService.getVendors(pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorRequest request) {
        return ResponseEntity.ok(vendorService.updateVendor(id, request));
    }
}
