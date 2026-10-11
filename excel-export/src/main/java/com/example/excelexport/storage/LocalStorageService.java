package com.example.excelexport.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Service("localStorageService")
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path storageDirectory;

    public LocalStorageService(@Value("${app.export.storage-dir:./temp-exports}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(this.storageDirectory)) {
                Files.createDirectories(this.storageDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local storage folder: " + storageDir, e);
        }
    }

    @Override
    public String storeFile(String storageKey, File file, String contentType) throws IOException {
        Path targetPath = storageDirectory.resolve(storageKey);
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored export object in local storage key: {} ({} bytes)", storageKey, file.length());
        return targetPath.toUri().toString();
    }

    @Override
    public String generateDownloadUrl(String storageKey, String originalFileName, Duration expiration) {
        return "/api/v1/exports/download/" + storageKey;
    }

    @Override
    public Resource loadResource(String storageKey) throws IOException {
        Path filePath = storageDirectory.resolve(storageKey).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundException("File not found or not readable: " + storageKey);
        }
        return new FileSystemResource(filePath.toFile());
    }

    @Override
    public boolean deleteFile(String storageKey) {
        try {
            Path filePath = storageDirectory.resolve(storageKey).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete storage file: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }
}
