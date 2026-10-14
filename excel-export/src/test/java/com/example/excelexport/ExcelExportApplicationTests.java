package com.example.excelexport;

import com.example.excelexport.dto.ExportJobResponse;
import com.example.excelexport.model.ExportJob;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.service.ExcelExportService;
import com.example.excelexport.service.JobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExcelExportApplicationTests {

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private JobStorageService jobStorageService;

    @Test
    void testAsyncExportFlow() throws Exception {
        String jobId = "test-job-" + System.currentTimeMillis();
        ExportJob job = jobStorageService.createJob(jobId);
        assertNotNull(job);
        assertEquals(ExportStatus.PENDING, job.getStatus());

        // Run export
        excelExportService.processExportAsync(jobId, null);

        // Wait up to 10 seconds for completion
        int attempts = 0;
        while (job.getStatus() != ExportStatus.COMPLETED && attempts < 20) {
            Thread.sleep(500);
            attempts++;
        }

        assertEquals(ExportStatus.COMPLETED, job.getStatus());
        assertTrue(job.getProcessedRecords() > 0);
        assertNotNull(job.getFilePath());

        File file = new File(job.getFilePath());
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        // Cleanup
        jobStorageService.removeJob(jobId);
        assertFalse(file.exists());
    }
}
