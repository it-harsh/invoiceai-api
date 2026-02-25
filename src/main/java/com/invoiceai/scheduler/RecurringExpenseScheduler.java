package com.invoiceai.scheduler;

import com.invoiceai.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseScheduler {

    private final RecurringExpenseService recurringExpenseService;

    /**
     * Runs daily at 1:00 AM UTC — processes all due recurring expenses.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void processDueRecurringExpenses() {
        log.info("Starting recurring expense processing...");
        int processed = recurringExpenseService.processDueRecurringExpenses();
        log.info("Recurring expense processing complete. {} expenses created.", processed);
    }
}
