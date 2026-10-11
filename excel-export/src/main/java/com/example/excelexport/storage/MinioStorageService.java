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
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@Service("minioStorageService")
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${app.storage.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.minio.bucket-name:export-bucket}")
    private String bucketName;

    @Value("${app.storage.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.storage.minio.secret-key:minioadmin}")
    private String secretKey;

    private S3Client minioClient;
    private S3Presigner minioPresigner;

    private synchronized S3Client getMinioClient() {
        if (minioClient == null) {
            minioClient = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return minioClient;
    }

    private synchronized S3Presigner getPresigner() {
        if (minioPresigner == null) {
            minioPresigner = S3Presigner.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return minioPresigner;
    }

    private void ensureBucketExists() {
        try {
            getMinioClient().headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            log.info("Bucket [{}] does not exist in MinIO. Creating it automatically...", bucketName);
            getMinioClient().createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("Bucket [{}] not found (HTTP 404). Creating it automatically...", bucketName);
                getMinioClient().createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            } else {
                log.warn("Warning checking MinIO bucket [{}]: {}", bucketName, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Unexpected exception inspecting MinIO bucket [{}]: {}", bucketName, e.getMessage());
        }
    }

    @Override
    public String storeFile(String storageKey, File file, String contentType) throws IOException {
        ensureBucketExists();
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .build();

        getMinioClient().putObject(putRequest, RequestBody.fromFile(file));
        log.info("Uploaded export file to MinIO bucket [{}] with key [{}] at {}", bucketName, storageKey, endpoint);
        return endpoint + "/" + bucketName + "/" + storageKey;
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
            log.warn("Could not generate MinIO presigned URL, falling back to direct key route: {}", e.getMessage());
            return "/api/v1/exports/download/" + storageKey;
        }
    }

    @Override
    public Resource loadResource(String storageKey) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build();

        ResponseInputStream<GetObjectResponse> stream = getMinioClient().getObject(getRequest);
        byte[] data = stream.readAllBytes();
        return new ByteArrayResource(data);
    }

    @Override
    public boolean deleteFile(String storageKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .build();
            getMinioClient().deleteObject(deleteRequest);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete MinIO object key: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.MINIO;
    }
}
