package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadUrlRequest {

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileType;

    @Positive
    private long fileSize;
}
