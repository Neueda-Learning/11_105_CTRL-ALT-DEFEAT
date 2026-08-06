package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentLifecycleService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentLifecycleService.class);

    private final PaymentRepository paymentRepository;
    private final HistoryService historyService;
    private final ScheduledExecutorService scheduler;
    private final TransactionTemplate transactionTemplate;

    public PaymentLifecycleService(
            PaymentRepository paymentRepository,
            HistoryService historyService,
            PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.historyService = historyService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void scheduleCompletion(String paymentId) {
        scheduler.schedule(() -> transactionTemplate.executeWithoutResult(status -> completeIfSent(paymentId)),
                30, TimeUnit.SECONDS);
    }

    private void completeIfSent(String paymentId) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SENT) {
                return;
            }
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            historyService.recordTransition(payment, PaymentStatus.SENT, PaymentStatus.COMPLETED,
                    "Auto-completed after 30 seconds", null, "SYSTEM");
        });
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            LOG.warn("Interrupted while shutting down payment lifecycle scheduler", ex);
        }
    }
}
