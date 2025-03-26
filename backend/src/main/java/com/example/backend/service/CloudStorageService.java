// src/main/java/com/example/backend/service/CloudStorageService.java
package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;

@Service
public class CloudStorageService {

    @Value("${cloud.b2.bucket}")
    private String bucketName;

    @Value("${cloud.b2.endpoint}")
    private String endpoint;

    @Value("${cloud.b2.region}")
    private String region;

    @Value("${cloud.b2.access-key}")
    private String accessKey;

    @Value("${cloud.b2.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .build();
    }

    public void uploadFile(String filename, MultipartFile file) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filename)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()));
    }
}
