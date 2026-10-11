package com.example.excelexport.queue;

import com.example.excelexport.service.ExcelExportService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class ExportQueueService {

    private static final Logger log = LoggerFactory.getLogger(ExportQueueService.class);
    private static final int MAX_RETRIES = 3;

    private final BlockingQueue<ExportTaskMessage> messageQueue = new LinkedBlockingQueue<>(500);
    private final ExecutorService consumerThreadPool = Executors.newFixedThreadPool(4);
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor();

    private final ExcelExportService excelExportService;
    private volatile boolean running = true;

    public ExportQueueService(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    @PostConstruct
    public void startConsumers() {
        log.info("Starting 4 background export queue worker consumers...");
        for (int i = 1; i <= 4; i++) {
            final int workerId = i;
            consumerThreadPool.submit(() -> processQueue(workerId));
        }
    }

    /**
     * Producer: Enqueues an export task message
     */
    public boolean enqueue(ExportTaskMessage message) {
        boolean offered = messageQueue.offer(message);
        if (offered) {
            log.info("Enqueued export task for Job ID {}", message.getJobId());
        } else {
            log.warn("Export queue is FULL! Rejected Job ID {}", message.getJobId());
        }
        return offered;
    }

    /**
     * Consumer loop: fetches messages and processes them with retry handling
     */
    private void processQueue(int workerId) {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                ExportTaskMessage message = messageQueue.poll(2, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }

                log.info("Worker-{} picked up Job ID: {} (Format: {})", workerId, message.getJobId(), message.getFormat());
                try {
                    excelExportService.processExport(message);
                } catch (Exception ex) {
                    log.error("Worker-{} encountered error on Job ID {}: {}", workerId, message.getJobId(), ex.getMessage());
                    handleRetry(message, ex);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected worker loop error in worker {}: {}", workerId, e.getMessage(), e);
            }
        }
    }

    /**
     * Retry mechanism with exponential backoff
     */
    private void handleRetry(ExportTaskMessage message, Exception ex) {
        if (message.getRetryCount() < MAX_RETRIES) {
            message.incrementRetryCount();
            long delaySeconds = (long) Math.pow(2, message.getRetryCount()) * 2; // 4s, 8s, 16s
            log.warn("Scheduling retry {} for Job ID {} in {} seconds...", message.getRetryCount(), message.getJobId(), delaySeconds);

            retryScheduler.schedule(() -> {
                messageQueue.offer(message);
            }, delaySeconds, TimeUnit.SECONDS);
        } else {
            log.error("Job ID {} exceeded maximum retry attempts ({}). Marked as FAILED.", message.getJobId(), MAX_RETRIES);
            excelExportService.markJobFailed(message.getJobId(), "Max retry attempts exceeded: " + ex.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        consumerThreadPool.shutdownNow();
        retryScheduler.shutdownNow();
    }
}
