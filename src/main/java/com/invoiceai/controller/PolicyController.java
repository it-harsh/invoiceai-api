package com.invoiceai.controller;

import com.invoiceai.dto.request.CreatePolicyRequest;
import com.invoiceai.dto.request.UpdatePolicyRequest;
import com.invoiceai.dto.response.PolicyResponse;
import com.invoiceai.dto.response.PolicyViolationResponse;
import com.invoiceai.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getPolicies() {
        return ResponseEntity.ok(policyService.getPolicies());
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable UUID id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/violations")
    public ResponseEntity<Page<PolicyViolationResponse>> getViolations(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(policyService.getViolations(pageable));
    }
}
