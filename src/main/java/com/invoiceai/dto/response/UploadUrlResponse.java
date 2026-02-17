package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadUrlResponse {
    private String uploadUrl;
    private String fileKey;
}
