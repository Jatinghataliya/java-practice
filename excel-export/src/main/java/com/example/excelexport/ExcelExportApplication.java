package com.example.excelexport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ExcelExportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelExportApplication.class, args);
    }
}
