package com.invoiceai.service;

import com.invoiceai.model.*;
import com.invoiceai.model.enums.MemberRole;
import com.invoiceai.repository.OrganizationMemberRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final OrganizationMemberRepository memberRepository;

    @Value("${app.email.from}")
    private String fromAddress;

    @Value("${app.email.base-url}")
    private String baseUrl;

    @Async
    public void sendPolicyViolationNotification(Expense expense, List<PolicyViolation> violations) {
        List<String> emails = getOrgAdminEmails(expense.getOrganization().getId());
        String subject = "Policy Violation: " + expense.getVendorName() + " - $" + expense.getAmount();
        String html = buildPolicyViolationHtml(expense, violations);

        for (String email : emails) {
            sendHtmlEmail(email, subject, html);
        }
    }

    @Async
    public void sendBudgetAlertNotification(Budget budget, BudgetAlert alert) {
        List<String> emails = getOrgAdminEmails(budget.getOrganization().getId());
        String categoryName = budget.getCategory() != null ? budget.getCategory().getName() : "Overall";
        String subject = "Budget Alert: " + categoryName + " at " + alert.getPercentage().intValue() + "%";
        String html = buildBudgetAlertHtml(budget, alert, categoryName);

        for (String email : emails) {
            sendHtmlEmail(email, subject, html);
        }
    }

    @Async
    public void sendExpenseExportEmail(User user, String subject, String htmlBody, byte[] csvAttachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment("expenses.csv", new ByteArrayResource(csvAttachment), "text/csv");
            mailSender.send(message);
            log.info("Export email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send export email to {}", user.getEmail(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {} subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} subject: {}", to, subject, e);
        }
    }

    private List<String> getOrgAdminEmails(UUID orgId) {
        return memberRepository.findByOrganizationIdAndRoleIn(orgId, List.of(MemberRole.OWNER, MemberRole.ADMIN))
                .stream()
                .map(m -> m.getUser().getEmail())
                .toList();
    }

    private String buildPolicyViolationHtml(Expense expense, List<PolicyViolation> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
        sb.append("<h2 style='color: #EF4444;'>Policy Violation Detected</h2>");
        sb.append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Vendor:</td><td style='padding: 8px;'>").append(expense.getVendorName()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Amount:</td><td style='padding: 8px;'>$").append(expense.getAmount()).append(" ").append(expense.getCurrency()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Date:</td><td style='padding: 8px;'>").append(expense.getDate()).append("</td></tr>");
        if (expense.getDescription() != null) {
            sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Description:</td><td style='padding: 8px;'>").append(expense.getDescription()).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<h3>Violations:</h3><ul>");
        for (PolicyViolation v : violations) {
            sb.append("<li style='color: #EF4444;'>").append(v.getViolationMessage()).append("</li>");
        }
        sb.append("</ul>");
        sb.append("<p><a href='").append(baseUrl).append("/expenses/").append(expense.getId()).append("' style='color: #3B82F6;'>View Expense</a></p>");
        sb.append("<hr style='border: none; border-top: 1px solid #E5E7EB; margin: 20px 0;'>");
        sb.append("<p style='color: #9CA3AF; font-size: 12px;'>InvoiceAI - Automated Notification</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildBudgetAlertHtml(Budget budget, BudgetAlert alert, String categoryName) {
        String color = alert.getPercentage().compareTo(BigDecimal.valueOf(100)) >= 0 ? "#EF4444" : "#F59E0B";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
        sb.append("<h2 style='color: ").append(color).append(";'>Budget Alert: ").append(categoryName).append("</h2>");
        sb.append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Budget:</td><td style='padding: 8px;'>$").append(budget.getMonthlyLimit()).append("/month</td></tr>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Actual Spend:</td><td style='padding: 8px;'>$").append(alert.getActualAmount()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Usage:</td><td style='padding: 8px; color: ").append(color).append("; font-weight: bold;'>").append(alert.getPercentage().intValue()).append("%</td></tr>");
        sb.append("</table>");
        // Progress bar
        int pct = Math.min(alert.getPercentage().intValue(), 100);
        sb.append("<div style='background: #E5E7EB; border-radius: 8px; height: 24px; width: 100%; max-width: 400px; margin: 16px 0;'>");
        sb.append("<div style='background: ").append(color).append("; border-radius: 8px; height: 24px; width: ").append(pct).append("%;'></div>");
        sb.append("</div>");
        sb.append("<p><a href='").append(baseUrl).append("/settings/budgets' style='color: #3B82F6;'>View Budgets</a></p>");
        sb.append("<hr style='border: none; border-top: 1px solid #E5E7EB; margin: 20px 0;'>");
        sb.append("<p style='color: #9CA3AF; font-size: 12px;'>InvoiceAI - Automated Notification</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
