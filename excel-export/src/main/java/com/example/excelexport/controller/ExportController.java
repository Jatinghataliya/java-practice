package com.example.excelexport.controller;

import com.example.excelexport.dto.ExportJobResponse;
import com.example.excelexport.dto.ExportRequest;
import com.example.excelexport.model.ExportFormat;
import com.example.excelexport.model.ExportJobEntity;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.queue.ExportQueueService;
import com.example.excelexport.queue.ExportTaskMessage;
import com.example.excelexport.service.EmployeeDataSeeder;
import com.example.excelexport.service.JobStorageService;
import com.example.excelexport.service.NotificationService;
import com.example.excelexport.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final JobStorageService jobStorageService;
    private final ExportQueueService exportQueueService;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final EmployeeDataSeeder dataSeeder;

    public ExportController(JobStorageService jobStorageService,
                            ExportQueueService exportQueueService,
                            StorageService storageService,
                            NotificationService notificationService,
                            EmployeeDataSeeder dataSeeder) {
        this.jobStorageService = jobStorageService;
        this.exportQueueService = exportQueueService;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.dataSeeder = dataSeeder;
    }

    /**
     * 1. Trigger asynchronous Export job (Distributed Queue Message Producer)
     * Returns HTTP 202 ACCEPTED with Job ID and Format.
     */
    @PostMapping("/generate")
    public ResponseEntity<ExportJobResponse> triggerExport(@RequestBody(required = false) ExportRequest request) {
        String jobId = UUID.randomUUID().toString();
        ExportFormat format = (request != null && request.getFormat() != null) ? request.getFormat() : ExportFormat.XLSX;
        String dept = request != null ? request.getDepartment() : null;
        Long maxRows = request != null ? request.getMaxRows() : null;

        // Persist initial job in JPA database
        ExportJobEntity job = jobStorageService.createJob(jobId, format, dept, maxRows);

        // Enqueue to worker queue
        ExportTaskMessage message = new ExportTaskMessage(jobId, format, dept, maxRows);
        boolean enqueued = exportQueueService.enqueue(message);

        if (!enqueued) {
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage("Export queue capacity exceeded. Please try again later.");
            jobStorageService.saveJob(job);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ExportJobResponse.fromEntity(job));
        }

        log.info("Triggered export job: {} [Format: {}]", jobId, format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ExportJobResponse.fromEntity(job));
    }

    /**
     * 2. Real-Time Server-Sent Events (SSE) Progress Notification Stream
     * Clients connect once and receive live progress broadcasts (0% -> 100%) and download link.
     */
    @GetMapping(value = "/notifications/sse/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeProgressStream(@PathVariable String jobId) {
        log.info("Client subscribed to SSE progress stream for Job ID: {}", jobId);
        return notificationService.subscribe(jobId);
    }

    /**
     * 3. Poll job status & progress percentage
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ExportJobResponse> getExportStatus(@PathVariable String jobId) {
        Optional<ExportJobEntity> jobOpt = jobStorageService.getJob(jobId);
        return jobOpt.map(job -> ResponseEntity.ok(ExportJobResponse.fromEntity(job)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 4. Download the generated file from Storage by storageKey or token
     */
    @GetMapping("/download/{storageKey}")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable String storageKey) {
        try {
            Resource resource = storageService.loadResource(storageKey);
            String contentType = storageKey.endsWith(".csv.gz") ? "application/gzip" :
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + storageKey + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 5. Cancel / remove an export job and delete stored file
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> cancelOrDeleteJob(@PathVariable String jobId) {
        boolean removed = jobStorageService.removeJob(jobId);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 6. Bulk seed sample records for testing
     */
    @PostMapping("/seed")
    public ResponseEntity<String> seedData(@RequestParam(defaultValue = "100000") int count) {
        long durationMs = dataSeeder.seedEmployees(count);
        return ResponseEntity.ok("Seeded " + count + " employees in " + durationMs + " ms.");
    }
}
