package com.example.excelexport;

import com.example.excelexport.model.ExportFormat;
import com.example.excelexport.model.ExportJobEntity;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.queue.ExportQueueService;
import com.example.excelexport.queue.ExportTaskMessage;
import com.example.excelexport.service.JobStorageService;
import com.example.excelexport.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExcelExportApplicationTests {

    @Autowired
    private ExportQueueService exportQueueService;

    @Autowired
    private JobStorageService jobStorageService;

    @Autowired
    private StorageService storageService;

    @Test
    void testAsyncExcelExportQueueFlow() throws Exception {
        String jobId = "test-job-xlsx-" + System.currentTimeMillis();
        ExportJobEntity job = jobStorageService.createJob(jobId, ExportFormat.XLSX, null, null);
        assertNotNull(job);
        assertEquals(ExportStatus.PENDING, job.getStatus());

        // Enqueue task to background queue worker
        boolean enqueued = exportQueueService.enqueue(new ExportTaskMessage(jobId, ExportFormat.XLSX, null, null));
        assertTrue(enqueued);

        // Wait up to 10 seconds for completion
        int attempts = 0;
        Optional<ExportJobEntity> completedJobOpt = jobStorageService.getJob(jobId);
        while (attempts < 20 && (completedJobOpt.isEmpty() || completedJobOpt.get().getStatus() != ExportStatus.COMPLETED)) {
            Thread.sleep(500);
            attempts++;
            completedJobOpt = jobStorageService.getJob(jobId);
        }

        assertTrue(completedJobOpt.isPresent());
        ExportJobEntity completedJob = completedJobOpt.get();
        assertEquals(ExportStatus.COMPLETED, completedJob.getStatus());
        assertTrue(completedJob.getProcessedRecords() > 0);
        assertNotNull(completedJob.getStorageKey());
        assertNotNull(completedJob.getDownloadUrl());

        // Test Storage Resource Load
        Resource resource = storageService.loadResource(completedJob.getStorageKey());
        assertTrue(resource.exists());
        assertTrue(resource.contentLength() > 0);

        // Cleanup
        jobStorageService.removeJob(jobId);
        assertFalse(resource.exists());
    }

    @Test
    void testGzippedCsvExportQueueFlow() throws Exception {
        String jobId = "test-job-csvgz-" + System.currentTimeMillis();
        ExportJobEntity job = jobStorageService.createJob(jobId, ExportFormat.CSV_GZ, null, null);
        assertNotNull(job);

        // Enqueue task
        exportQueueService.enqueue(new ExportTaskMessage(jobId, ExportFormat.CSV_GZ, null, null));

        // Wait for completion
        int attempts = 0;
        Optional<ExportJobEntity> completedJobOpt = jobStorageService.getJob(jobId);
        while (attempts < 20 && (completedJobOpt.isEmpty() || completedJobOpt.get().getStatus() != ExportStatus.COMPLETED)) {
            Thread.sleep(500);
            attempts++;
            completedJobOpt = jobStorageService.getJob(jobId);
        }

        assertTrue(completedJobOpt.isPresent());
        ExportJobEntity completedJob = completedJobOpt.get();
        assertEquals(ExportStatus.COMPLETED, completedJob.getStatus());
        assertTrue(completedJob.getFileName().endsWith(".csv.gz"));

        // Storage load check
        Resource resource = storageService.loadResource(completedJob.getStorageKey());
        assertTrue(resource.exists());
        assertTrue(resource.contentLength() > 0);

        // Cleanup
        jobStorageService.removeJob(jobId);
        assertFalse(resource.exists());
    }
}
