package com.example.excelexport.model;

import java.time.LocalDateTime;

public class ExportJob {

    private String jobId;
    private ExportStatus status;
    private long totalRecords;
    private long processedRecords;
    private int percentage;
    private String fileName;
    private String filePath;
    private long fileSizeBytes;
    private String downloadUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public ExportJob() {
        this.status = ExportStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public ExportJob(String jobId) {
        this.jobId = jobId;
        this.status = ExportStatus.PENDING;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
