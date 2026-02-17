package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {
    private UserResponse user;
    private List<OrganizationResponse> organizations;
    private String accessToken;
    private String refreshToken;
}
