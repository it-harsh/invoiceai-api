package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssistantChatResponse {
    private String reply;
}
