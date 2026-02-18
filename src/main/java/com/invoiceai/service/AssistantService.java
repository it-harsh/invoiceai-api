package com.invoiceai.service;

import com.invoiceai.dto.request.AssistantChatRequest;
import com.invoiceai.model.Expense;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.security.TenantContext;
import com.invoiceai.service.ai.AiChatService;
import com.invoiceai.service.ai.AiChatService.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final AiChatService aiChatService;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    private static final int MAX_HISTORY_SIZE = 20;

    @Transactional(readOnly = true)
    public String chat(AssistantChatRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();
        String systemPrompt = buildSystemPrompt(orgId);

        List<ChatMessage> messages = new ArrayList<>();
        if (request.getHistory() != null) {
            for (var entry : request.getHistory()) {
                messages.add(new ChatMessage(entry.getRole(), entry.getContent()));
            }
        }
        messages.add(new ChatMessage("user", request.getMessage()));

        // Truncate to prevent token overflow
        if (messages.size() > MAX_HISTORY_SIZE) {
            messages = new ArrayList<>(messages.subList(messages.size() - MAX_HISTORY_SIZE, messages.size()));
        }

        try {
            return aiChatService.chat(systemPrompt, messages);
        } catch (Exception e) {
            log.error("AI chat failed for org {}", orgId, e);
            return "I'm sorry, I'm having trouble processing your request right now. Please try again in a moment.";
        }
    }

    private String buildSystemPrompt(UUID orgId) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);

        BigDecimal currentMonthSpend = expenseRepository.sumApprovedAmountByDateRange(orgId, monthStart, now);
        long currentMonthCount = expenseRepository.countApprovedByDateRange(orgId, monthStart, now);

        BigDecimal lastMonthSpend = expenseRepository.sumApprovedAmountByDateRange(orgId, lastMonthStart, lastMonthEnd);
        long lastMonthCount = expenseRepository.countApprovedByDateRange(orgId, lastMonthStart, lastMonthEnd);

        List<Object[]> byCategory = expenseRepository.sumByCategoryAndDateRange(orgId, monthStart, now);
        List<Object[]> topVendors = expenseRepository.topVendors(orgId, monthStart, now, PageRequest.of(0, 5));

        var categories = categoryRepository.findByOrganizationId(orgId);
        long pendingCount = expenseRepository.countPendingReview(orgId);

        List<Expense> recentExpenses = expenseRepository.findRecentByOrganizationId(orgId, PageRequest.of(0, 10));

        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are an AI expense assistant for InvoiceAI. You help users understand their expense data.
                Answer questions concisely and accurately based on the data provided below.
                If you cannot answer from the data, say so honestly. Do not make up numbers.
                Format currency amounts with $ and two decimal places.
                Use bullet points or short paragraphs. Keep answers under 200 words unless the user asks for detail.
                Today's date is %s.

                === EXPENSE DATA CONTEXT ===

                """.formatted(now));

        sb.append("CURRENT MONTH (%s to %s):\n".formatted(monthStart, now));
        sb.append("- Total approved spend: $%s\n".formatted(currentMonthSpend));
        sb.append("- Number of approved expenses: %d\n".formatted(currentMonthCount));
        sb.append("- Expenses pending review: %d\n".formatted(pendingCount));
        sb.append("\n");

        sb.append("LAST MONTH (%s to %s):\n".formatted(lastMonthStart, lastMonthEnd));
        sb.append("- Total approved spend: $%s\n".formatted(lastMonthSpend));
        sb.append("- Number of approved expenses: %d\n".formatted(lastMonthCount));
        sb.append("\n");

        if (!byCategory.isEmpty()) {
            sb.append("SPEND BY CATEGORY (current month):\n");
            for (Object[] row : byCategory) {
                sb.append("- %s: $%s\n".formatted(row[0], row[2]));
            }
            sb.append("\n");
        }

        if (!topVendors.isEmpty()) {
            sb.append("TOP VENDORS (current month):\n");
            for (Object[] row : topVendors) {
                sb.append("- %s: $%s (%s expenses)\n".formatted(row[0], row[1], row[2]));
            }
            sb.append("\n");
        }

        if (!recentExpenses.isEmpty()) {
            sb.append("RECENT EXPENSES (last 10):\n");
            for (Expense e : recentExpenses) {
                String catName = e.getCategory() != null ? e.getCategory().getName() : "Uncategorized";
                sb.append("- %s | %s | $%s | %s | %s\n".formatted(
                        e.getDate(), e.getVendorName(), e.getAmount(), catName, e.getStatus()));
            }
            sb.append("\n");
        }

        if (!categories.isEmpty()) {
            sb.append("AVAILABLE CATEGORIES: ");
            sb.append(categories.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
            sb.append("\n");
        }

        return sb.toString();
    }
}
