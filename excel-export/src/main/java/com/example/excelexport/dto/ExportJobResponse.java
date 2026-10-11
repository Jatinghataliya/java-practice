package com.example.excelexport.dto;

import com.example.excelexport.model.ExportFormat;
import com.example.excelexport.model.ExportJobEntity;
import com.example.excelexport.model.ExportStatus;

import java.time.LocalDateTime;

public class ExportJobResponse {

    private String jobId;
    private ExportStatus status;
    private ExportFormat format;
    private long totalRecords;
    private long processedRecords;
    private int percentage;
    private String fileName;
    private long fileSizeBytes;
    private String downloadUrl;
    private String downloadToken;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public ExportJobResponse() {
    }

    public static ExportJobResponse fromEntity(ExportJobEntity entity) {
        ExportJobResponse res = new ExportJobResponse();
        res.setJobId(entity.getJobId());
        res.setStatus(entity.getStatus());
        res.setFormat(entity.getFormat());
        res.setTotalRecords(entity.getTotalRecords());
        res.setProcessedRecords(entity.getProcessedRecords());
        res.setPercentage(entity.getPercentage());
        res.setFileName(entity.getFileName());
        res.setFileSizeBytes(entity.getFileSizeBytes());
        res.setDownloadUrl(entity.getDownloadUrl());
        res.setDownloadToken(entity.getDownloadToken());
        res.setErrorMessage(entity.getErrorMessage());
        res.setCreatedAt(entity.getCreatedAt());
        res.setCompletedAt(entity.getCompletedAt());
        return res;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
