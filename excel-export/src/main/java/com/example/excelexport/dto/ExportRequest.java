package com.example.excelexport.dto;

import com.example.excelexport.model.ExportFormat;

public class ExportRequest {

    private ExportFormat format = ExportFormat.XLSX;
    private String department;
    private Long maxRows;

    public ExportRequest() {
    }

    public ExportRequest(ExportFormat format, String department, Long maxRows) {
        this.format = format != null ? format : ExportFormat.XLSX;
        this.department = department;
        this.maxRows = maxRows;
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
}
