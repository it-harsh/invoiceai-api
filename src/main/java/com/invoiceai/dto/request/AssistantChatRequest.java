package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssistantChatRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;

    private List<MessageEntry> history;

    @Getter
    @Setter
    public static class MessageEntry {
        private String role;
        private String content;
    }
}
