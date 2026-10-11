package com.example.excelexport.storage;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service("gcsStorageService")
public class GcsStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);

    @Value("${app.storage.gcs.bucket-name:export-bucket}")
    private String bucketName;

    private Storage storageClient;

    private synchronized Storage getStorageClient() {
        if (storageClient == null) {
            storageClient = StorageOptions.getDefaultInstance().getService();
        }
        return storageClient;
    }

    @Override
    public String storeFile(String storageKey, File file, String contentType) throws IOException {
        BlobId blobId = BlobId.of(bucketName, storageKey);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        byte[] bytes = Files.readAllBytes(file.toPath());
        getStorageClient().create(blobInfo, bytes);

        log.info("Uploaded export file to Google Cloud Storage bucket [{}] with key [{}]", bucketName, storageKey);
        return "gs://" + bucketName + "/" + storageKey;
    }

    @Override
    public String generateDownloadUrl(String storageKey, String originalFileName, Duration expiration) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, storageKey)).build();
            long durationMillis = (expiration != null ? expiration : Duration.ofHours(2)).toMillis();

            return getStorageClient().signUrl(
                    blobInfo,
                    durationMillis,
                    TimeUnit.MILLISECONDS,
                    Storage.SignUrlOption.withV4Signature()
            ).toString();
        } catch (Exception e) {
            log.warn("Could not generate GCS signed URL, falling back to direct key route: {}", e.getMessage());
            return "/api/v1/exports/download/" + storageKey;
        }
    }

    @Override
    public Resource loadResource(String storageKey) throws IOException {
        Blob blob = getStorageClient().get(BlobId.of(bucketName, storageKey));
        if (blob == null || !blob.exists()) {
            throw new IOException("GCS Blob not found: " + storageKey);
        }
        return new InputStreamResource(Channels.newInputStream(blob.reader()));
    }

    @Override
    public boolean deleteFile(String storageKey) {
        try {
            return getStorageClient().delete(BlobId.of(bucketName, storageKey));
        } catch (Exception e) {
            log.error("Failed to delete GCS blob: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.GCS;
    }
}
