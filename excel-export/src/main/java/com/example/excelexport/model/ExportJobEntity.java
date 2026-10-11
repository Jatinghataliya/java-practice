package com.example.excelexport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_jobs", indexes = {
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_created", columnList = "created_at")
})
public class ExportJobEntity {

    @Id
    @Column(name = "job_id", length = 64, nullable = false, unique = true)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ExportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 20, nullable = false)
    private ExportFormat format = ExportFormat.XLSX;

    @Column(name = "department_filter", length = 100)
    private String departmentFilter;

    @Column(name = "max_rows_limit")
    private Long maxRowsLimit;

    @Column(name = "total_records")
    private long totalRecords;

    @Column(name = "processed_records")
    private long processedRecords;

    @Column(name = "progress_percentage")
    private int percentage;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Column(name = "download_token", length = 128)
    private String downloadToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public ExportJobEntity() {
        this.status = ExportStatus.PENDING;
        this.format = ExportFormat.XLSX;
        this.createdAt = LocalDateTime.now();
    }

    public ExportJobEntity(String jobId) {
        this.jobId = jobId;
        this.status = ExportStatus.PENDING;
        this.format = ExportFormat.XLSX;
        this.createdAt = LocalDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public String getDepartmentFilter() {
        return departmentFilter;
    }

    public void setDepartmentFilter(String departmentFilter) {
        this.departmentFilter = departmentFilter;
    }

    public Long getMaxRowsLimit() {
        return maxRowsLimit;
    }

    public void setMaxRowsLimit(Long maxRowsLimit) {
        this.maxRowsLimit = maxRowsLimit;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
        calculatePercentage();
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(long processedRecords) {
        this.processedRecords = processedRecords;
        calculatePercentage();
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    private void calculatePercentage() {
        if (totalRecords > 0) {
            this.percentage = (int) Math.min(100, (processedRecords * 100) / totalRecords);
        } else {
            this.percentage = 0;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadToken() {
        return downloadToken;
    }

    public void setDownloadToken(String downloadToken) {
        this.downloadToken = downloadToken;
    }

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
