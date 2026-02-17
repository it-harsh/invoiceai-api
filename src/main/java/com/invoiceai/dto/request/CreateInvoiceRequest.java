package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInvoiceRequest {

    @NotBlank
    private String fileKey;

    @NotBlank
    private String fileName;

    @Positive
    private long fileSize;

    @NotBlank
    private String fileType;
}
