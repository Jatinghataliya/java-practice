package com.example.excelexport.queue;

import com.example.excelexport.model.ExportFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ExportTaskMessage implements Serializable {

    private String jobId;
    private ExportFormat format;
    private String department;
    private Long maxRows;
    private int retryCount;
    private LocalDateTime queuedAt;

    public ExportTaskMessage() {
        this.queuedAt = LocalDateTime.now();
    }

    public ExportTaskMessage(String jobId, ExportFormat format, String department, Long maxRows) {
        this.jobId = jobId;
        this.format = format != null ? format : ExportFormat.XLSX;
        this.department = department;
        this.maxRows = maxRows;
        this.retryCount = 0;
        this.queuedAt = LocalDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(Long maxRows) {
        this.maxRows = maxRows;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }
}
