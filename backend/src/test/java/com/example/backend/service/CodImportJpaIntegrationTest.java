package com.example.backend.service;

import com.example.backend.model.CodQueryStatus;
import com.example.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class CodImportJpaIntegrationTest {
    @Autowired CodImportService service;
    @Autowired CodQueryRepository queries;
    @Autowired CodEntryRepository entries;
    @MockBean CodCsvSource source;
    @TempDir Path temporary;

    @BeforeEach void clean(){queries.deleteAllInBatch();entries.deleteAllInBatch();}
    Path csv(int rows,boolean invalidLast) throws Exception {
        Path file=Files.createTempFile(temporary,"cod-",".csv");
        try(var writer=Files.newBufferedWriter(file)) {
            writer.write("file,mineral,formula,compoundsource,year,authors,journal,doi\n");
            for(int i=0;i<rows;i++)writer.write(i+",mineral,"+(invalidLast && i==rows-1?"X".repeat(600):"H2 O")+",H O,2026,author,journal,doi\n");
        }
        return file;
    }
    void awaitTerminal(){assertTimeoutPreemptively(Duration.ofSeconds(60),()->{
        while(queries.findAll().stream().anyMatch(q->q.getStatus()==CodQueryStatus.PENDING || q.getStatus()==CodQueryStatus.RUNNING))Thread.sleep(20);
    });}
    @Test void insertsAndUpdatesAcrossIndependentBatches() throws Exception {
        when(source.download(any())).thenReturn(csv(1501,false));
        service.checkAndImport(List.of("H"));awaitTerminal();
        assertThat(entries.count()).isEqualTo(1501);
        assertThat(queries.findAll()).allMatch(q->q.getStatus()==CodQueryStatus.COMPLETED && q.getProgress()==100);
        when(source.download(any())).thenReturn(csv(1501,false));
        service.checkAndImport(List.of("N"));awaitTerminal();
        assertThat(entries.count()).isEqualTo(1501);
        assertThat(queries.findAll()).hasSize(2).allMatch(q->q.getStatus()==CodQueryStatus.COMPLETED);
    }
    @Test void failingBatchRollsBackWithoutRollingBackPriorBatchesAndPersistsFailed() throws Exception {
        when(source.download(any())).thenReturn(csv(501,true));
        service.checkAndImport(List.of("O"));awaitTerminal();
        assertThat(entries.count()).isEqualTo(500);
        assertThat(queries.findAll()).hasSize(1).allMatch(q->q.getStatus()==CodQueryStatus.FAILED);
        assertThat(service.checkAndImport(List.of("O")).getStatus()).isEqualTo(CodQueryStatus.FAILED);
    }
}
