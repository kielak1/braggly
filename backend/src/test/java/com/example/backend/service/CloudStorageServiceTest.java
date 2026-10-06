package com.example.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CloudStorageServiceTest {
    @TempDir Path temporary;
    @Test void multipartStreamIsClosedEvenWhenSdkFails() throws Exception {
        var sdk=mock(S3Client.class);var service=new CloudStorageService();
        ReflectionTestUtils.setField(service,"s3Client",sdk);ReflectionTestUtils.setField(service,"bucketName","synthetic-bucket");
        var input=new CifInfoServiceTest.TrackingStream(new byte[20]);var file=mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(input);when(file.getSize()).thenReturn(20L);
        when(sdk.putObject(any(PutObjectRequest.class),any(RequestBody.class))).thenThrow(new IllegalStateException("synthetic SDK failure"));
        assertThatThrownBy(() -> service.uploadFile("synthetic.raw",file)).isInstanceOf(IllegalStateException.class);
        assertThat(input.closed).isTrue();service.close();verify(sdk).close();
    }
    @Test void diskUploadHasKnownLengthAndCanStreamDataWithoutHeapCopies() throws Exception {
        var sdk=mock(S3Client.class);var service=new CloudStorageService();
        ReflectionTestUtils.setField(service,"s3Client",sdk);ReflectionTestUtils.setField(service,"bucketName","synthetic-bucket");
        Path file=temporary.resolve("synthetic.cif");Files.writeString(file,"synthetic CIF body");
        when(sdk.putObject(any(PutObjectRequest.class),any(RequestBody.class))).thenAnswer(call -> {
            RequestBody body=call.getArgument(1);assertThat(body.contentLength()).isEqualTo(Files.size(file));
            try(var stream=body.contentStreamProvider().newStream()){assertThat(new String(stream.readAllBytes())).isEqualTo("synthetic CIF body");}
            return null;
        });
        service.uploadFile("cif/123.cif",file);verify(sdk).putObject(any(PutObjectRequest.class),any(RequestBody.class));
        assertThat(Files.exists(file)).isTrue(); // Caller owns the temporary file.
    }
}
