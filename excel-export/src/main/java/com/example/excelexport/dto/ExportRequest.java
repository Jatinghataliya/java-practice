package com.example.excelexport.dto;

public class ExportRequest {

    private String department;
    private Long maxRows;

    public ExportRequest() {
    }

    public ExportRequest(String department, Long maxRows) {
        this.department = department;
        this.maxRows = maxRows;
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
