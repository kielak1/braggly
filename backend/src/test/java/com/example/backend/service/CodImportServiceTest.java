package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class CodImportServiceTest {
    @TempDir Path temporary;
    CodEntryRepository entries;
    CodQueryRepository queries;
    CodCsvSource source;
    CodImportService service;
    EntityManager entityManager;
    Map<Long, CodQuery> stored;
    CountDownLatch release;

    @BeforeEach void setup() {
        entries = mock(CodEntryRepository.class); queries = mock(CodQueryRepository.class);
        source = mock(CodCsvSource.class); entityManager = mock(EntityManager.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        stored = new ConcurrentHashMap<>(); AtomicLong sequence = new AtomicLong();
        when(queries.save(any())).thenAnswer(call -> {
            CodQuery q = call.getArgument(0); if(q.getId() == null) q.setId(sequence.incrementAndGet());
            stored.put(q.getId(),q); return q;
        });
        when(queries.saveAndFlush(any())).thenAnswer(call -> queries.save(call.getArgument(0)));
        when(queries.findRecentCompletedQueries(any())).thenAnswer(call -> stored.values().stream().filter(CodQuery::isCompleted).toList());
        when(queries.findFirstByElementSetAndRequestedAtAfterOrderByRequestedAtDesc(anyString(),any())).thenAnswer(call ->
                stored.values().stream().filter(q -> q.getElementSet().equals(call.getArgument(0)))
                        .max(Comparator.comparing(CodQuery::getRequestedAt)));
        service = new CodImportService(entries,queries,source,entityManager,transactions,500);
        release = new CountDownLatch(1);
    }

    @AfterEach void shutdown() { release.countDown(); service.close(); }

    Path csv(int rows) throws IOException {
        Path file=Files.createTempFile(temporary,"cod-",".csv");
        try(var writer=Files.newBufferedWriter(file)) {
            writer.write("file,mineral,formula,compoundsource,year,authors,journal,doi\n");
            for(int i=0;i<rows;i++) writer.write(i+",mineral,H2 O,H O,2026,author,journal,doi\n");
        }
        return file;
    }

    void awaitTerminal() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            while(stored.values().stream().anyMatch(q -> q.getStatus()==CodQueryStatus.PENDING || q.getStatus()==CodQueryStatus.RUNNING)) Thread.sleep(10);
        });
    }

    @Test void atomicallyDeduplicatesConcurrentNormalizedRequests() throws Exception {
        Path file=csv(1); CountDownLatch started=new CountDownLatch(1);
        when(source.download(any())).thenAnswer(call -> {started.countDown(); release.await(); return file;});
        try(var clients=Executors.newFixedThreadPool(12)) {
            List<Future<?>> calls=new ArrayList<>();
            for(int i=0;i<24;i++) calls.add(clients.submit(() -> service.checkAndImport(List.of("O","H","H"))));
            for(var call:calls) call.get(5,TimeUnit.SECONDS);
            assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();
            assertThat(stored).hasSize(1);
            verify(source,times(1)).download(List.of("H","O"));
        }
        release.countDown(); awaitTerminal();
        assertThat(stored.values()).allMatch(q -> q.getStatus()==CodQueryStatus.COMPLETED);
    }

    @Test void hasOneActiveTwoQueuedAndControlledRejectionWithFailedStatus() throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        when(source.download(any())).thenAnswer(call -> {started.countDown(); release.await(); return csv(1);});
        service.checkAndImport(List.of("H")); assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();
        service.checkAndImport(List.of("N")); service.checkAndImport(List.of("C"));
        assertThatThrownBy(() -> service.checkAndImport(List.of("O"))).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(429));
        assertThat(stored.values().stream().filter(q -> q.getStatus()==CodQueryStatus.RUNNING).count()).isEqualTo(1);
        assertThat(stored.values().stream().filter(q -> q.getStatus()==CodQueryStatus.PENDING).count()).isEqualTo(2);
        assertThat(stored.values().stream().filter(q -> q.getStatus()==CodQueryStatus.FAILED).count()).isEqualTo(1);
        assertThat(service.checkAndImport(List.of("O")).getStatus()).isEqualTo(CodQueryStatus.FAILED);
        verify(source,times(1)).download(any());
        release.countDown(); awaitTerminal();
        assertThat(stored.values().stream().filter(CodQuery::isCompleted).count()).isEqualTo(3);
    }

    @Test void completesLargeInputInBoundedBatchesAndDeletesItsFile() throws Exception {
        Path file=csv(10000); when(source.download(any())).thenReturn(file);
        AtomicInteger rows=new AtomicInteger(); AtomicInteger largest=new AtomicInteger();
        when(entries.saveAll(any())).thenAnswer(call -> {
            List<CodEntry> batch=call.getArgument(0); rows.addAndGet(batch.size()); largest.accumulateAndGet(batch.size(),Math::max);
            return List.of();
        });
        service.checkAndImport(List.of("H")); awaitTerminal();
        assertThat(rows.get()).isEqualTo(10000); assertThat(largest.get()).isEqualTo(500);
        verify(entityManager,times(20)).flush(); verify(entityManager,times(20)).clear();
        assertThat(Files.exists(file)).isFalse();
        assertThat(service.checkAndImport(List.of("H")).getStatus()).isEqualTo(CodQueryStatus.COMPLETED);
    }

    @Test void ioAndTimeoutFailuresBecomeFailedAndPollingDoesNotRestartThem() throws Exception {
        when(source.download(any())).thenThrow(new SocketTimeoutException("synthetic timeout"));
        service.checkAndImport(List.of("H")); awaitTerminal();
        assertThat(service.checkAndImport(List.of("H")).getStatus()).isEqualTo(CodQueryStatus.FAILED);
        verify(source,times(1)).download(any());
        doReturn(csv(1)).when(source).download(any());
        service.checkAndImport(List.of("H"),true); awaitTerminal();
        assertThat(stored.values().stream().filter(CodQuery::isCompleted).count()).isEqualTo(1);
    }

    @Test void databaseFailureRollsBackBatchAndRecordsFailedInSeparateTransaction() throws Exception {
        Path file=csv(1); when(source.download(any())).thenReturn(file);
        when(entries.saveAll(any())).thenThrow(new DataAccessResourceFailureException("synthetic DB failure"));
        service.checkAndImport(List.of("H")); awaitTerminal();
        assertThat(stored.values()).allMatch(q -> q.getStatus()==CodQueryStatus.FAILED);
        verify(entityManager).clear(); assertThat(Files.exists(file)).isFalse();
    }

    @Test void unexpectedRuntimeExceptionBecomesFailed() throws Exception {
        when(source.download(any())).thenThrow(new IllegalStateException("synthetic failure"));
        service.checkAndImport(List.of("H")); awaitTerminal();
        assertThat(stored.values()).allMatch(q -> q.getStatus()==CodQueryStatus.FAILED);
    }

    @Test void retainsFailedTerminalStatusUntilDatabaseRecoversWithoutRestartingWork() throws Exception {
        when(source.download(any())).thenThrow(new IOException("synthetic download failure"));
        AtomicBoolean unavailable = new AtomicBoolean(true);
        doAnswer(call -> {
            CodQuery query = call.getArgument(0);
            if (query.getStatus() == CodQueryStatus.FAILED && unavailable.get()) {
                throw new DataAccessResourceFailureException("synthetic status DB failure");
            }
            return queries.save(query);
        }).when(queries).saveAndFlush(any());
        service.checkAndImport(List.of("H")); awaitTerminal();
        assertThatThrownBy(() -> service.checkAndImport(List.of("H")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
        unavailable.set(false);
        assertThat(service.checkAndImport(List.of("H")).getStatus()).isEqualTo(CodQueryStatus.FAILED);
        verify(source, times(1)).download(any());
    }

    @Test void retriesTerminalStatusAutomaticallyAfterDatabaseRecovery() throws Exception {
        when(source.download(any())).thenThrow(new IOException("synthetic download failure"));
        AtomicBoolean unavailable = new AtomicBoolean(true);
        AtomicInteger persistedFailed = new AtomicInteger();
        doAnswer(call -> {
            CodQuery query = call.getArgument(0);
            if (query.getStatus() == CodQueryStatus.FAILED) {
                if (unavailable.get()) throw new DataAccessResourceFailureException("synthetic DB outage");
                persistedFailed.incrementAndGet();
            }
            return queries.save(query);
        }).when(queries).saveAndFlush(any());
        service.checkAndImport(List.of("H")); awaitTerminal();
        synchronized (service) { unavailable.set(false); }
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            while (persistedFailed.get() == 0) Thread.sleep(20);
        });
        verify(source, times(1)).download(any());
    }

    @Test void shutdownFailsQueuedJobsAndInterruptsActiveJob() throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        when(source.download(any())).thenAnswer(call -> {started.countDown(); release.await(); return csv(1);});
        service.checkAndImport(List.of("H")); assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();
        service.checkAndImport(List.of("N")); service.checkAndImport(List.of("C"));
        service.close(); awaitTerminal();
        assertThat(stored.values()).allMatch(q -> q.getStatus()==CodQueryStatus.FAILED);
    }
}
