package com.invoiceai.controller;

import com.invoiceai.dto.response.OrganizationResponse;
import com.invoiceai.security.UserPrincipal;
import com.invoiceai.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<Map<String, List<OrganizationResponse>>> getOrganizations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<OrganizationResponse> orgs = organizationService.getUserOrganizations(principal.getUser().getId());
        return ResponseEntity.ok(Map.of("organizations", orgs));
    }
}
