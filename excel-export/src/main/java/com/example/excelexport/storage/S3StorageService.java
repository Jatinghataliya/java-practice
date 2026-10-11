package com.example.excelexport.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

@Service("s3StorageService")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${app.storage.s3.bucket-name:export-bucket}")
    private String bucketName;

    @Value("${app.storage.s3.region:us-east-1}")
    private String region;

    @Value("${app.storage.s3.access-key:}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key:}")
    private String secretKey;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    private synchronized S3Client getS3Client() {
        if (s3Client == null) {
            var builder = S3Client.builder().region(Region.of(region));
            if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
            }
            s3Client = builder.build();
        }
        return s3Client;
    }

    private synchronized S3Presigner getPresigner() {
        if (s3Presigner == null) {
            var builder = S3Presigner.builder().region(Region.of(region));
            if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
            }
            s3Presigner = builder.build();
        }
        return s3Presigner;
    }

    @Override
    public String storeFile(String storageKey, File file, String contentType) throws IOException {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .build();

        getS3Client().putObject(putRequest, RequestBody.fromFile(file));
        log.info("Uploaded export file to AWS S3 bucket [{}] with key [{}]", bucketName, storageKey);
        return "s3://" + bucketName + "/" + storageKey;
    }

    @Override
    public String generateDownloadUrl(String storageKey, String originalFileName, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .responseContentDisposition("attachment; filename=\"" + originalFileName + "\"")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration != null ? expiration : Duration.ofHours(2))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return getPresigner().presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Could not generate AWS S3 presigned URL, falling back to direct key route: {}", e.getMessage());
            return "/api/v1/exports/download/" + storageKey;
        }
    }

    @Override
    public Resource loadResource(String storageKey) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build();

        ResponseInputStream<GetObjectResponse> s3Stream = getS3Client().getObject(getRequest);
        byte[] data = s3Stream.readAllBytes();
        return new ByteArrayResource(data);
    }

    @Override
    public boolean deleteFile(String storageKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .build();
            getS3Client().deleteObject(deleteRequest);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete S3 object key: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.S3;
    }
}
