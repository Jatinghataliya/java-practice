package com.example.excelexport.service;

import com.example.excelexport.dto.JobProgressEvent;
import com.example.excelexport.model.ExportFormat;
import com.example.excelexport.model.ExportJobEntity;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.queue.ExportTaskMessage;
import com.example.excelexport.repository.EmployeeRepository;
import com.example.excelexport.storage.StorageService;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

@Service
public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);

    private final EmployeeRepository employeeRepository;
    private final JobStorageService jobStorageService;
    private final StorageService storageService;
    private final NotificationService notificationService;

    @Value("${app.export.storage-dir:./temp-exports}")
    private String tempStorageDir;

    @Value("${app.export.batch-fetch-size:2000}")
    private int fetchSize;

    @Value("${app.export.rows-per-sheet:1000000}")
    private int rowsPerSheet;

    public ExcelExportService(EmployeeRepository employeeRepository,
                              JobStorageService jobStorageService,
                              StorageService storageService,
                              NotificationService notificationService) {
        this.employeeRepository = employeeRepository;
        this.jobStorageService = jobStorageService;
        this.storageService = storageService;
        this.notificationService = notificationService;
    }

    /**
     * Entrypoint executed by Queue Worker Consumer
     */
    public void processExport(ExportTaskMessage message) throws Exception {
        String jobId = message.getJobId();
        Optional<ExportJobEntity> jobOpt = jobStorageService.getJob(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Export Job ID {} not found in persistent repository.", jobId);
            return;
        }

        ExportJobEntity job = jobOpt.get();
        job.setStatus(ExportStatus.IN_PROGRESS);
        job.setStartedAt(LocalDateTime.now());
        jobStorageService.saveJob(job);

        broadcastProgress(job);

        File tempLocalFile = null;
        try {
            Path tempDir = Paths.get(tempStorageDir);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            ExportFormat format = message.getFormat() != null ? message.getFormat() : ExportFormat.XLSX;
            String fileName = "employees_export_" + jobId + format.getFileExtension();
            tempLocalFile = tempDir.resolve("temp_" + jobId + format.getFileExtension()).toFile();

            // 1. Count matching rows baseline
            String dept = message.getDepartment();
            Long maxRows = message.getMaxRows();
            long totalRecords = employeeRepository.countEmployees(dept, maxRows);
            job.setTotalRecords(totalRecords);
            jobStorageService.saveJob(job);

            log.info("Processing Job ID {} [Format: {}]. Total rows to export: {}", jobId, format, totalRecords);

            // 2. Generate file based on format
            if (format == ExportFormat.CSV_GZ) {
                generateGzippedCsv(job, tempLocalFile, dept, maxRows, totalRecords);
            } else {
                generateExcelXlsx(job, tempLocalFile, dept, maxRows, totalRecords);
            }

            // 3. Upload to Object Storage / Blob Storage
            String storageKey = jobId + format.getFileExtension();
            storageService.storeFile(storageKey, tempLocalFile, format.getContentType());

            // 4. Generate download token and URL
            String downloadToken = jobStorageService.generateDownloadToken(jobId);
            String downloadUrl = storageService.generateDownloadUrl(storageKey, fileName, Duration.ofHours(2));

            job.setProcessedRecords(job.getTotalRecords());
            job.setStatus(ExportStatus.COMPLETED);
            job.setFileName(fileName);
            job.setStorageKey(storageKey);
            job.setFileSizeBytes(tempLocalFile.length());
            job.setDownloadUrl(downloadUrl);
            job.setDownloadToken(downloadToken);
            job.setCompletedAt(LocalDateTime.now());
            jobStorageService.saveJob(job);

            broadcastProgress(job);
            log.info("Successfully finished export Job ID: {}. Stored file: {} ({} bytes)",
                    jobId, storageKey, tempLocalFile.length());

        } finally {
            if (tempLocalFile != null && tempLocalFile.exists()) {
                tempLocalFile.delete(); // Delete local temp staging file
            }
        }
    }

    private void generateExcelXlsx(ExportJobEntity job, File outputFile, String dept, Long maxRows, long totalRecords) throws Exception {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile), 64 * 1024)) {
            Workbook workbook = new Workbook(os, "EmployeeService", "1.0");

            final int[] sheetIndex = {1};
            final int[] currentRowInSheet = {1};
            final long[] totalProcessed = {0};

            final Worksheet[] currentWorksheet = {workbook.newWorksheet("Employees Part " + sheetIndex[0])};
            writeExcelHeaders(currentWorksheet[0]);

            if (totalRecords > 0) {
                employeeRepository.streamEmployees(dept, maxRows, fetchSize, emp -> {
                    if (currentRowInSheet[0] > rowsPerSheet) {
                        try {
                            currentWorksheet[0].finish();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        sheetIndex[0]++;
                        currentWorksheet[0] = workbook.newWorksheet("Employees Part " + sheetIndex[0]);
                        writeExcelHeaders(currentWorksheet[0]);
                        currentRowInSheet[0] = 1;
                    }

                    Worksheet ws = currentWorksheet[0];
                    int r = currentRowInSheet[0]++;

                    ws.value(r, 0, emp.getId() != null ? emp.getId() : 0);
                    ws.value(r, 1, emp.getFirstName() != null ? emp.getFirstName() : "");
                    ws.value(r, 2, emp.getLastName() != null ? emp.getLastName() : "");
                    ws.value(r, 3, emp.getEmail() != null ? emp.getEmail() : "");
                    ws.value(r, 4, emp.getDepartment() != null ? emp.getDepartment() : "");
                    ws.value(r, 5, emp.getSalary() != null ? emp.getSalary().doubleValue() : 0.0);
                    ws.value(r, 6, emp.getJoinDate() != null ? emp.getJoinDate().toString() : "");

                    totalProcessed[0]++;

                    if (totalProcessed[0] % 10000 == 0 || totalProcessed[0] == totalRecords) {
                        job.setProcessedRecords(totalProcessed[0]);
                        jobStorageService.saveJob(job);
                        broadcastProgress(job);
                    }
                });
            }

            currentWorksheet[0].finish();
            workbook.finish();
        }
    }

    private void generateGzippedCsv(ExportJobEntity job, File outputFile, String dept, Long maxRows, long totalRecords) throws Exception {
        try (OutputStream fos = new FileOutputStream(outputFile);
             OutputStream bos = new BufferedOutputStream(fos, 64 * 1024);
             GZIPOutputStream gzos = new GZIPOutputStream(bos);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8), 64 * 1024)) {

            // CSV Header
            writer.write("ID,First Name,Last Name,Email,Department,Salary,Join Date\n");

            final long[] totalProcessed = {0};

            if (totalRecords > 0) {
                employeeRepository.streamEmployees(dept, maxRows, fetchSize, emp -> {
                    try {
                        writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%s\n",
                                emp.getId() != null ? emp.getId() : 0,
                                escapeCsv(emp.getFirstName()),
                                escapeCsv(emp.getLastName()),
                                escapeCsv(emp.getEmail()),
                                escapeCsv(emp.getDepartment()),
                                emp.getSalary() != null ? emp.getSalary().doubleValue() : 0.0,
                                emp.getJoinDate() != null ? emp.getJoinDate().toString() : ""
                        ));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    totalProcessed[0]++;

                    if (totalProcessed[0] % 20000 == 0 || totalProcessed[0] == totalRecords) {
                        job.setProcessedRecords(totalProcessed[0]);
                        jobStorageService.saveJob(job);
                        broadcastProgress(job);
                    }
                });
            }

            writer.flush();
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    private void writeExcelHeaders(Worksheet ws) {
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

    public void markJobFailed(String jobId, String errorMessage) {
        Optional<ExportJobEntity> jobOpt = jobStorageService.getJob(jobId);
        if (jobOpt.isPresent()) {
            ExportJobEntity job = jobOpt.get();
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            jobStorageService.saveJob(job);
            broadcastProgress(job);
        }
    }

    private void broadcastProgress(ExportJobEntity job) {
        JobProgressEvent event = new JobProgressEvent(
                job.getJobId(),
                job.getStatus(),
                job.getTotalRecords(),
                job.getProcessedRecords(),
                job.getPercentage(),
                job.getDownloadUrl(),
                job.getErrorMessage()
        );
        notificationService.notifyProgress(event);
    }
}
