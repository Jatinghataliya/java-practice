package com.example.excelexport.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service("azureBlobStorageService")
public class AzureBlobStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

    @Value("${app.storage.azure.connection-string:}")
    private String connectionString;

    @Value("${app.storage.azure.container-name:export-container}")
    private String containerName;

    private BlobContainerClient containerClient;

    private synchronized BlobContainerClient getContainerClient() {
        if (containerClient == null) {
            if (connectionString == null || connectionString.isBlank()) {
                throw new IllegalStateException("Azure Blob connection string is not configured in application.yml");
            }
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
            }
        }
        return containerClient;
    }

    @Override
    public String storeFile(String storageKey, File file, String contentType) throws IOException {
        BlobClient blobClient = getContainerClient().getBlobClient(storageKey);
        blobClient.uploadFromFile(file.getAbsolutePath(), true);
        log.info("Uploaded export file to Azure Blob container [{}] with key [{}]", containerName, storageKey);
        return blobClient.getBlobUrl();
    }

    @Override
    public String generateDownloadUrl(String storageKey, String originalFileName, Duration expiration) {
        try {
            BlobClient blobClient = getContainerClient().getBlobClient(storageKey);
            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                    OffsetDateTime.now().plus(expiration != null ? expiration : Duration.ofHours(2)),
                    permission
            );
            String sasToken = blobClient.generateSas(sasValues);
            return blobClient.getBlobUrl() + "?" + sasToken;
        } catch (Exception e) {
            log.warn("Could not generate Azure SAS URL, falling back to direct key route: {}", e.getMessage());
            return "/api/v1/exports/download/" + storageKey;
        }
    }

    @Override
    public Resource loadResource(String storageKey) throws IOException {
        BlobClient blobClient = getContainerClient().getBlobClient(storageKey);
        try (var is = blobClient.openInputStream()) {
            return new ByteArrayResource(is.readAllBytes());
        }
    }

    @Override
    public boolean deleteFile(String storageKey) {
        try {
            BlobClient blobClient = getContainerClient().getBlobClient(storageKey);
            return Boolean.TRUE.equals(blobClient.deleteIfExists());
        } catch (Exception e) {
            log.error("Failed to delete Azure blob: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.AZURE;
    }
}
