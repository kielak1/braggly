package com.example.backend.service;
import com.example.backend.model.User;
import com.example.backend.repository.XrdFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class XrdFileServiceTest {
    @Test void closesHeaderStreamAfterReadingMultipartUpload() throws Exception {
        var repo=mock(XrdFileRepository.class);var storage=mock(CloudStorageService.class);
        var service=new XrdFileService(repo,storage);var file=mock(MultipartFile.class);
        var input=new CifInfoServiceTest.TrackingStream("_SAMPLE='synthetic'\n1 2\n".getBytes());
        when(file.getInputStream()).thenReturn(input);var user=new User();user.setId(1L);
        service.saveUxdFile(file,"synthetic",false,user);assertThat(input.closed).isTrue();
    }
}
