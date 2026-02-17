package com.invoiceai.service.ai;

public interface AiExtractionService {
    ExtractionResult extract(byte[] fileBytes, String fileType, String fileName);
}
