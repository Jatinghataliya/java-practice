package com.example.excelexport.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${app.storage.type:local}")
    private String configuredStorageType;

    @Bean
    @Primary
    public StorageService primaryStorageService(
            LocalStorageService localStorageService,
            S3StorageService s3StorageService,
            MinioStorageService minioStorageService,
            AzureBlobStorageService azureBlobStorageService,
            GcsStorageService gcsStorageService) {

        StorageService selected = switch (configuredStorageType.toLowerCase().trim()) {
            case "s3" -> s3StorageService;
            case "minio" -> minioStorageService;
            case "azure" -> azureBlobStorageService;
            case "gcs" -> gcsStorageService;
            default -> localStorageService;
        };

        log.info("Active Storage Provider initialized: [{}]", selected.getStorageType());
        return selected;
    }
}
