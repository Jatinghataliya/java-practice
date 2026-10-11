package com.example.excelexport.dto;

import com.example.excelexport.model.ExportStatus;

import java.time.LocalDateTime;

public class JobProgressEvent {

    private String jobId;
    private ExportStatus status;
    private long totalRecords;
    private long processedRecords;
    private int percentage;
    private String downloadUrl;
    private String errorMessage;
    private LocalDateTime timestamp;

    public JobProgressEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public JobProgressEvent(String jobId, ExportStatus status, long totalRecords, long processedRecords, int percentage, String downloadUrl, String errorMessage) {
        this.jobId = jobId;
        this.status = status;
        this.totalRecords = totalRecords;
        this.processedRecords = processedRecords;
        this.percentage = percentage;
        this.downloadUrl = downloadUrl;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
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
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(long processedRecords) {
        this.processedRecords = processedRecords;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
