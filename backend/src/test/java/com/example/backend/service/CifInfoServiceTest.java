package com.example.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.EmptyColumnException;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CifInfoServiceTest {
    static byte[] cif(int atoms) {
        StringBuilder text=new StringBuilder("data_synthetic\n_chemical_formula_sum 'H2 O'\nloop_\n_atom_site_label\n_atom_site_type_symbol\n_atom_site_fract_x\n_atom_site_fract_y\n_atom_site_fract_z\n");
        for(int i=0;i<atoms;i++) text.append("H").append(i).append(" H 0.1 0.2 0.3\n");
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }
    static class TrackingStream extends ByteArrayInputStream {
        boolean closed;
        TrackingStream(byte[] bytes){super(bytes);}
        @Override public void close(){closed=true;}
    }
    CifInfoService service(CloudStorageService storage,long bytes,int atoms,int concurrency) {
        return new CifInfoService(storage,new CifLimits(bytes,atoms,concurrency,15000,60000));
    }
    @Test void parsesCachedCifAndClosesTheResponseStream() throws Exception {
        var storage=mock(CloudStorageService.class);var input=new TrackingStream(cif(3));
        when(storage.downloadFile(anyString())).thenReturn(input);
        var info=service(storage,4096,100,2).getStructureInfo("123");
        assertThat((List<?>)info.get("atoms")).hasSize(3);assertThat(input.closed).isTrue();
        verify(storage,never()).uploadFile(anyString(),any(Path.class));
    }
    @Test void rejectsOversizeCachedInputAndClosesIt() throws Exception {
        var storage=mock(CloudStorageService.class);var input=new TrackingStream(cif(3));
        when(storage.downloadFile(anyString())).thenReturn(input);
        assertThatThrownBy(() -> service(storage,100,100,2).getStructureInfo("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
        assertThat(input.closed).isTrue();
    }
    @Test void unsupportedAtomColumnsReturn422CloseStreamAndReleasePermit() throws Exception {
        var storage=mock(CloudStorageService.class);
        byte[] missingType=("data_synthetic\nloop_\n_atom_site_label\n_atom_site_fract_x\n"
                + "_atom_site_fract_y\n_atom_site_fract_z\nUnknown1 0.1 0.2 0.3\n").getBytes(StandardCharsets.UTF_8);
        var input=new TrackingStream(missingType);
        when(storage.downloadFile(anyString())).thenReturn(input,new TrackingStream(cif(1)));
        var service=service(storage,4096,100,1);
        assertThatThrownBy(() -> service.getStructureInfo("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(422);
                    assertThat(e.getReason()).isEqualTo("CIF atom data is not supported");
                });
        assertThat(input.closed).isTrue();
        assertThat((List<?>)service.getStructureInfo("456").get("atoms")).hasSize(1);
        verify(storage,never()).uploadFile(anyString(),any(Path.class));
    }
    @Test void libraryEmptyColumnExceptionReturns422AndClosesStreamWithoutLeakingPermit() throws Exception {
        var storage=mock(CloudStorageService.class);var input=new TrackingStream(cif(1));
        when(storage.downloadFile(anyString())).thenReturn(input,new TrackingStream(cif(1)));
        var service=service(storage,4096,100,1);
        try(var library=mockStatic(CifIO.class)) {
            library.when(() -> CifIO.readFromInputStream(any(InputStream.class)))
                    .thenThrow(new EmptyColumnException("synthetic missing column"));
            assertThatThrownBy(() -> service.getStructureInfo("123"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
        }
        assertThat(input.closed).isTrue();
        assertThat((List<?>)service.getStructureInfo("456").get("atoms")).hasSize(1);
    }
    @ParameterizedTest
    @ValueSource(strings={"data_synthetic\nloop_\n_atom_site_label\n_atom_site_fract_x\nC1\n", "", "not a CIF"})
    void malformedCifReturns422AndReleasesResources(String text) throws Exception {
        var storage=mock(CloudStorageService.class);var input=new TrackingStream(text.getBytes(StandardCharsets.UTF_8));
        when(storage.downloadFile(anyString())).thenReturn(input,new TrackingStream(cif(1)));
        var service=service(storage,4096,100,1);
        assertThatThrownBy(() -> service.getStructureInfo("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
        assertThat(input.closed).isTrue();
        assertThat((List<?>)service.getStructureInfo("456").get("atoms")).hasSize(1);
    }
    @Test void missingCoordinatesAre422InsteadOfEmptyOrPartialAtoms() throws Exception {
        var storage=mock(CloudStorageService.class);
        byte[] missing=cif(1);
        missing=new String(missing,StandardCharsets.UTF_8).replace("_atom_site_fract_z\n","")
                .replace("H0 H 0.1 0.2 0.3", "H0 H 0.1 0.2").getBytes(StandardCharsets.UTF_8);
        when(storage.downloadFile(anyString())).thenReturn(new TrackingStream(missing));
        assertThatThrownBy(() -> service(storage,4096,100,1).getStructureInfo("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
    }
    @Test void rejectsTooManyAtomsBeforeBuildingResponseAndClosesStream() throws Exception {
        var storage=mock(CloudStorageService.class);var input=new TrackingStream(cif(3));
        when(storage.downloadFile(anyString())).thenReturn(input);
        assertThatThrownBy(() -> service(storage,4096,2,2).getStructureInfo("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
        assertThat(input.closed).isTrue();
    }
    @Test void sizeLimitDuringTextParsingRemains413WhenLibraryWrapsTheIOException() throws Exception {
        var storage=mock(CloudStorageService.class);
        byte[] bytes=(new String(cif(3), StandardCharsets.UTF_8)+("#"+"x".repeat(100)+"\n").repeat(1000))
                .getBytes(StandardCharsets.UTF_8);
        var input=new TrackingStream(bytes);
        when(storage.downloadFile(anyString())).thenReturn(input);
        assertThatThrownBy(() -> service(storage,16384,100,2).getStructureInfo("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
        assertThat(input.closed).isTrue();
    }
    @Test void closesOnReadFailureAndReleasesConcurrencyPermit() throws Exception {
        var storage=mock(CloudStorageService.class);var broken=new InputStream(){
            boolean closed;
            @Override public int read() throws IOException {throw new IOException("synthetic read failure");}
            @Override public void close(){closed=true;}
        };
        when(storage.downloadFile(anyString())).thenReturn(broken,new TrackingStream(cif(1)));
        var service=service(storage,4096,100,1);
        assertThatThrownBy(() -> service.getStructureInfo("123")).isInstanceOf(RuntimeException.class);
        assertThat(broken.closed).isTrue();
        assertThat((List<?>)service.getStructureInfo("123").get("atoms")).hasSize(1);
    }
    @Test void enforcesConcurrentOperationLimitWithoutWaitingOrSpawningThreads() throws Exception {
        var storage=mock(CloudStorageService.class);var service=service(storage,4096,100,2);
        var started=new CountDownLatch(2);var release=new CountDownLatch(1);
        when(storage.downloadFile(anyString())).thenAnswer(call -> {started.countDown();release.await();return new TrackingStream(cif(1));});
        try(var clients=Executors.newFixedThreadPool(2)) {
            var first=clients.submit(() -> service.getStructureInfo("123"));var second=clients.submit(() -> service.getStructureInfo("456"));
            try {
                assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> service.getStructureInfo("789")).isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(429));
            } finally {release.countDown();}
            first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
        }
        assertThat((List<?>)service.getStructureInfo("789").get("atoms")).hasSize(1);
    }
    @Test void usesOneTemporaryFileForUnknownLengthUploadAndDeletesItOnSuccess() throws Exception {
        testTemporaryUpload(false);
    }
    @Test void deletesTemporaryFileAndClosesSourceWhenB2UploadFails() throws Exception {
        testTemporaryUpload(true);
    }
    void testTemporaryUpload(boolean fail) throws Exception {
        var storage=mock(CloudStorageService.class);when(storage.downloadFile(anyString())).thenThrow(new IOException("missing"));
        var input=new TrackingStream(cif(3));var connection=mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(input);when(connection.getContentLengthLong()).thenReturn(-1L);
        var service=spy(service(storage,4096,100,2));doReturn(connection).when(service).openCodConnection(anyString());
        AtomicReference<Path> uploaded=new AtomicReference<>();
        doAnswer(call -> {Path file=call.getArgument(1);uploaded.set(file);assertThat(Files.readAllBytes(file)).isEqualTo(cif(3));
            if(fail)throw new IllegalStateException("synthetic B2 failure");return null;
        }).when(storage).uploadFile(anyString(),any(Path.class));
        if(fail)assertThatThrownBy(() -> service.getStructureInfo("123")).isInstanceOf(IllegalStateException.class);
        else assertThat((List<?>)service.getStructureInfo("123").get("atoms")).hasSize(3);
        assertThat(input.closed).isTrue();assertThat(uploaded.get()).isNotNull();assertThat(Files.exists(uploaded.get())).isFalse();
        verify(storage,times(1)).downloadFile(anyString());
    }
    @Test void oversizeUnknownLengthSourceIsClosedWithoutUploading() throws Exception {
        var storage=mock(CloudStorageService.class);when(storage.downloadFile(anyString())).thenThrow(new IOException("missing"));
        var input=new TrackingStream(cif(3));var connection=mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(input);when(connection.getContentLengthLong()).thenReturn(-1L);
        var service=spy(service(storage,100,100,2));doReturn(connection).when(service).openCodConnection(anyString());
        assertThatThrownBy(() -> service.getStructureInfo("123")).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
        assertThat(input.closed).isTrue();verify(storage,never()).uploadFile(anyString(),any(Path.class));
    }
}
