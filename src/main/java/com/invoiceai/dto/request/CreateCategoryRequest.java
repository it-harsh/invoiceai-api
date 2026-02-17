package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Must be a valid hex color")
    private String color;

    @Size(max = 50)
    private String icon;
}
