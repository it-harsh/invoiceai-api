package com.invoiceai.service.ai;

import java.util.List;

public interface AiChatService {

    String chat(String systemPrompt, List<ChatMessage> conversationHistory);

    record ChatMessage(String role, String content) {}
}
