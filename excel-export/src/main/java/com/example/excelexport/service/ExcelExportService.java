package com.example.excelexport.service;

import com.example.excelexport.dto.ExportRequest;
import com.example.excelexport.model.ExportJob;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.repository.EmployeeRepository;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);

    private final EmployeeRepository employeeRepository;
    private final JobStorageService jobStorageService;

    @Value("${app.export.storage-dir:./temp-exports}")
    private String storageDir;

    @Value("${app.export.batch-fetch-size:2000}")
    private int fetchSize;

    @Value("${app.export.rows-per-sheet:1000000}")
    private int rowsPerSheet;

    public ExcelExportService(EmployeeRepository employeeRepository, JobStorageService jobStorageService) {
        this.employeeRepository = employeeRepository;
        this.jobStorageService = jobStorageService;
    }

    @Async("excelExportExecutor")
    public void processExportAsync(String jobId, ExportRequest request) {
        ExportJob job = jobStorageService.getJob(jobId);
        if (job == null) {
            log.error("Export Job ID {} not found in store.", jobId);
            return;
        }

        job.setStatus(ExportStatus.IN_PROGRESS);
        job.setStartedAt(LocalDateTime.now());

        File outputFile = null;
        try {
            // 1. Prepare export destination directory
            Path exportPath = Paths.get(storageDir);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            String fileName = "employees_export_" + jobId + ".xlsx";
            outputFile = exportPath.resolve(fileName).toFile();

            // 2. Count total records to determine progress baseline
            String dept = request != null ? request.getDepartment() : null;
            Long maxRows = request != null ? request.getMaxRows() : null;
            long totalRecords = employeeRepository.countEmployees(dept, maxRows);
            job.setTotalRecords(totalRecords);
            log.info("Starting Excel generation for Job ID {}. Total matching rows: {}", jobId, totalRecords);

            if (totalRecords == 0) {
                // Generate empty workbook with headers only
                generateEmptyWorkbook(outputFile);
                finalizeJob(job, outputFile, fileName, 0);
                return;
            }

            // 3. FastExcel Streamed Writing
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile), 64 * 1024)) {
                Workbook workbook = new Workbook(os, "EmployeeService", "1.0");

                final int[] sheetIndex = {1};
                final int[] currentRowInSheet = {1}; // Row 0 is header
                final long[] totalProcessed = {0};

                final Worksheet[] currentWorksheet = {workbook.newWorksheet("Employees Part " + sheetIndex[0])};
                writeHeaders(currentWorksheet[0]);

                // Stream rows from database via JDBC cursor
                employeeRepository.streamEmployees(dept, maxRows, fetchSize, emp -> {
                    // Check if current worksheet has reached row limit (e.g. 1,000,000 rows)
                    if (currentRowInSheet[0] > rowsPerSheet) {
                        try {
                            currentWorksheet[0].finish(); // Flushes current worksheet
                        } catch (java.io.IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                        sheetIndex[0]++;
                        currentWorksheet[0] = workbook.newWorksheet("Employees Part " + sheetIndex[0]);
                        writeHeaders(currentWorksheet[0]);
                        currentRowInSheet[0] = 1;
                    }

                    Worksheet ws = currentWorksheet[0];
                    int r = currentRowInSheet[0]++;

                    // Populate row cells
                    ws.value(r, 0, emp.getId() != null ? emp.getId() : 0);
                    ws.value(r, 1, emp.getFirstName() != null ? emp.getFirstName() : "");
                    ws.value(r, 2, emp.getLastName() != null ? emp.getLastName() : "");
                    ws.value(r, 3, emp.getEmail() != null ? emp.getEmail() : "");
                    ws.value(r, 4, emp.getDepartment() != null ? emp.getDepartment() : "");
                    ws.value(r, 5, emp.getSalary() != null ? emp.getSalary().doubleValue() : 0.0);
                    ws.value(r, 6, emp.getJoinDate() != null ? emp.getJoinDate().toString() : "");

                    totalProcessed[0]++;

                    // Update progress periodically (every 10,000 rows)
                    if (totalProcessed[0] % 10000 == 0 || totalProcessed[0] == totalRecords) {
                        job.setProcessedRecords(totalProcessed[0]);
                        log.debug("Job {}: processed {}/{} rows ({}%)", jobId, totalProcessed[0], totalRecords, job.getPercentage());
                    }
                });

                // Finalize last worksheet and workbook
                currentWorksheet[0].finish();
                workbook.finish();

                finalizeJob(job, outputFile, fileName, totalProcessed[0]);
                log.info("Completed Excel export for Job ID {}. Generated file: {} ({} bytes)",
                        jobId, fileName, outputFile.length());
            }

        } catch (Exception ex) {
            log.error("Failed to generate Excel for Job ID {}: {}", jobId, ex.getMessage(), ex);
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage("Export failed: " + ex.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
        }
    }

    private void writeHeaders(Worksheet ws) {
        ws.value(0, 0, "ID");
        ws.value(0, 1, "First Name");
        ws.value(0, 2, "Last Name");
        ws.value(0, 3, "Email");
        ws.value(0, 4, "Department");
        ws.value(0, 5, "Salary");
        ws.value(0, 6, "Join Date");
        ws.style(0, 0).bold().set();
        ws.style(0, 1).bold().set();
        ws.style(0, 2).bold().set();
        ws.style(0, 3).bold().set();
        ws.style(0, 4).bold().set();
        ws.style(0, 5).bold().set();
        ws.style(0, 6).bold().set();
    }

    private void generateEmptyWorkbook(File outputFile) throws Exception {
        try (OutputStream os = new FileOutputStream(outputFile)) {
            Workbook workbook = new Workbook(os, "EmployeeService", "1.0");
            Worksheet ws = workbook.newWorksheet("Employees");
            writeHeaders(ws);
            ws.finish();
            workbook.finish();
        }
    }

    private void finalizeJob(ExportJob job, File outputFile, String fileName, long processedRows) {
        job.setProcessedRecords(processedRows);
        job.setStatus(ExportStatus.COMPLETED);
        job.setFileName(fileName);
        job.setFilePath(outputFile.getAbsolutePath());
        job.setFileSizeBytes(outputFile.length());
        job.setDownloadUrl("/api/v1/exports/download/" + job.getJobId());
        job.setCompletedAt(LocalDateTime.now());
    }
}
