package com.invoiceai.controller;

import com.invoiceai.dto.request.AssistantChatRequest;
import com.invoiceai.dto.response.AssistantChatResponse;
import com.invoiceai.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        String reply = assistantService.chat(request);
        return ResponseEntity.ok(AssistantChatResponse.builder().reply(reply).build());
    }
}
