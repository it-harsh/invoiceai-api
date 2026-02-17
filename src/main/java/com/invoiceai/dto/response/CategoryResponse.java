package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String color;
    private String icon;
    private boolean isDefault;
}
