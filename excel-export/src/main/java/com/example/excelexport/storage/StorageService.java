package com.example.excelexport.storage;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

public interface StorageService {

    /**
     * Store the exported file into object storage (e.g. S3 / MinIO / Local Disk Blob)
     *
     * @param storageKey  Unique object key in storage bucket
     * @param file        Local generated file
     * @param contentType MIME type of the file
     * @return Stored resource identifier / URI
     */
    String storeFile(String storageKey, File file, String contentType) throws IOException;

    /**
     * Generate an expiring pre-signed URL or authenticated download token link
     *
     * @param storageKey       Storage object key
     * @param originalFileName Display filename for download attachment
     * @param expiration       Duration for link validity
     * @return Full download URL
     */
    String generateDownloadUrl(String storageKey, String originalFileName, Duration expiration);

    /**
     * Load the resource from storage for streaming
     */
    Resource loadResource(String storageKey) throws IOException;

    /**
     * Delete file from storage
     */
    boolean deleteFile(String storageKey);
}
