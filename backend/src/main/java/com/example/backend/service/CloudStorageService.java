// src/main/java/com/example/backend/service/CloudStorageService.java
package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.ByteArrayOutputStream;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
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

                s3Client.putObject(request, RequestBody.fromInputStream(
                                file.getInputStream(), file.getSize()));
        }

        public void deleteFile(String filename) {
                DeleteObjectRequest request = DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(filename)
                                .build();

                s3Client.deleteObject(request);
        }

        public InputStream downloadFile(String filename) throws IOException {
                // Tworzymy żądanie pobrania pliku z S3/Backblaze B2
                GetObjectRequest request = GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(filename)
                                .build();

                // Pobieramy plik i zwracamy jego zawartość jako InputStream
                return s3Client.getObject(request);
        }

        public void uploadInputStream(String filename, InputStream inputStream) {
                try {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[8192];
                        int nRead;
                        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                        }
                        byte[] bytes = buffer.toByteArray();

                        PutObjectRequest request = PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(filename)
                                        .contentType("text/plain")
                                        .build();

                        s3Client.putObject(request, RequestBody.fromBytes(bytes));
                } catch (IOException e) {
                        throw new RuntimeException("Błąd podczas przesyłania pliku do B2", e);
                }
        }

}
