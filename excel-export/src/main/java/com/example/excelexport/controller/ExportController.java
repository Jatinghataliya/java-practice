package com.example.excelexport.controller;

import com.example.excelexport.dto.ExportJobResponse;
import com.example.excelexport.dto.ExportRequest;
import com.example.excelexport.model.ExportJob;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.service.ExcelExportService;
import com.example.excelexport.service.JobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final JobStorageService jobStorageService;
    private final ExcelExportService excelExportService;
    private final com.example.excelexport.service.EmployeeDataSeeder dataSeeder;

    public ExportController(JobStorageService jobStorageService,
                            ExcelExportService excelExportService,
                            com.example.excelexport.service.EmployeeDataSeeder dataSeeder) {
        this.jobStorageService = jobStorageService;
        this.excelExportService = excelExportService;
        this.dataSeeder = dataSeeder;
    }

    /**
     * 1. Trigger asynchronous Excel export job
     * Returns HTTP 202 ACCEPTED with Job ID immediately without blocking.
     */
    @PostMapping("/generate")
    public ResponseEntity<ExportJobResponse> triggerExport(@RequestBody(required = false) ExportRequest request) {
        String jobId = UUID.randomUUID().toString();
        ExportJob job = jobStorageService.createJob(jobId);

        // Fire asynchronous background worker
        excelExportService.processExportAsync(jobId, request);

        log.info("Queued export job: {}", jobId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ExportJobResponse.fromJob(job));
    }

    /**
     * 2. Poll job status & progress percentage
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ExportJobResponse> getExportStatus(@PathVariable String jobId) {
        ExportJob job = jobStorageService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(ExportJobResponse.fromJob(job));
    }

    /**
     * 3. Download the generated Excel file once COMPLETED
     */
    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable String jobId) {
        ExportJob job = jobStorageService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (job.getStatus() != ExportStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Export-Status", job.getStatus().name())
                    .build();
        }

        File file = new File(job.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource = new FileSystemResource(file);
        String downloadName = job.getFileName() != null ? job.getFileName() : "employees.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length())
                .body(resource);
    }

    /**
     * 4. Cancel / remove an export job
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
     * 5. Bulk seed sample records for testing
     */
    @PostMapping("/seed")
    public ResponseEntity<String> seedData(@RequestParam(defaultValue = "100000") int count) {
        long durationMs = dataSeeder.seedEmployees(count);
        return ResponseEntity.ok("Seeded " + count + " employees in " + durationMs + " ms.");
    }
}
