package com.example.backend.service;

import com.example.backend.dto.CodQueryStatusResponse;
import com.example.backend.model.CodEntry;
import com.example.backend.model.CodQuery;
import com.example.backend.model.CodQueryStatus;
import com.example.backend.repository.CodEntryRepository;
import com.example.backend.repository.CodQueryRepository;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class CodImportService {
    private static final Logger log = LoggerFactory.getLogger(CodImportService.class);
    private final CodEntryRepository entries;
    private final CodQueryRepository queries;
    private final CodCsvSource source;
    private final EntityManager entityManager;
    private final TransactionTemplate batchTransaction;
    private final TransactionTemplate statusTransaction;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(2), runnable -> {
                Thread thread = new Thread(runnable, "cod-import");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    // At most three admitted jobs. Failed terminal writes stay here until DB recovery.
    private final Map<String, ImportJob> admitted = new HashMap<>();
    private final ScheduledExecutorService statusRecovery = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "cod-status-recovery");
        thread.setDaemon(true);
        return thread;
    });
    private boolean recoveryScheduled;
    private boolean stopping;
    private final int batchSize;

    public CodImportService(CodEntryRepository entries, CodQueryRepository queries, CodCsvSource source,
            EntityManager entityManager, PlatformTransactionManager transactions,
            @Value("${cod.import.batch-size:500}") int batchSize) {
        if (batchSize < 1 || batchSize > 10000) throw new IllegalArgumentException("Invalid COD batch size");
        this.entries = entries;
        this.queries = queries;
        this.source = source;
        this.entityManager = entityManager;
        this.batchSize = batchSize;
        batchTransaction = new TransactionTemplate(transactions);
        statusTransaction = new TransactionTemplate(transactions);
        statusTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CodQueryStatusResponse checkAndImport(List<String> elements) {
        return checkAndImport(elements, false);
    }

    // Admission, normalized-key deduplication and enqueue are one atomic operation.
    // The production service is a single replica; this lock does not coordinate replicas.
    public synchronized CodQueryStatusResponse checkAndImport(List<String> elements, boolean retry) {
        List<String> normalized = elements.stream().map(String::trim).distinct().sorted().toList();
        if (normalized.isEmpty() || normalized.stream().anyMatch(e -> !e.matches("[A-Z][a-z]?"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid element set");
        }
        String key = String.join(",", normalized);
        flushTerminalStates();
        ImportJob existingJob = admitted.get(key);
        if (existingJob != null) return response(existingJob.query);
        if (stopping) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "COD importer stopping");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(240);
        Set<String> requested = new HashSet<>(normalized);
        for (CodQuery query : queries.findRecentCompletedQueries(cutoff)) {
            if (requested.containsAll(query.getElementsList())) return response(query);
        }
        Optional<CodQuery> previous = queries.findFirstByElementSetAndRequestedAtAfterOrderByRequestedAtDesc(key, cutoff);
        if (previous.isPresent() && previous.get().getStatus() == CodQueryStatus.FAILED && !retry) {
            return response(previous.get());
        }
        if (admitted.size() >= 3) {
            CodQuery rejected = new CodQuery(key, LocalDateTime.now(), false);
            rejected.setStatus(CodQueryStatus.FAILED);
            statusTransaction.executeWithoutResult(status -> queries.saveAndFlush(rejected));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "COD import queue is full");
        }
        CodQuery query = queries.save(new CodQuery(key, LocalDateTime.now(), false));
        ImportJob job = new ImportJob(normalized, query);
        try {
            admitted.put(key, job);
            executor.execute(job);
        } catch (RejectedExecutionException rejected) {
            // Persist rejection too: it must never remain PENDING.
            query.setStatus(CodQueryStatus.FAILED);
            if (saveStatus(query)) admitted.remove(key, job);
            else scheduleStatusRecovery();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "COD import queue is full");
        }
        return response(query);
    }

    private CodQueryStatusResponse response(CodQuery query) {
        return new CodQueryStatusResponse(query.getStatus(), query.getRequestedAt(), query.getProgress());
    }

    private boolean saveStatus(CodQuery query) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                statusTransaction.executeWithoutResult(status -> queries.saveAndFlush(query));
                return true;
            } catch (RuntimeException failure) {
                log.warn("COD status persistence failed for job {} ({})", query.getId(), failure.getClass().getSimpleName());
            }
        }
        return false;
    }

    private void flushTerminalStates() {
        admitted.values().removeIf(job -> job.terminal() && saveStatus(job.query));
        // No new work is admitted while a terminal status cannot be persisted.
        if (admitted.values().stream().anyMatch(ImportJob::terminal)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "COD status database unavailable");
        }
    }

    private synchronized void finish(ImportJob job, CodQueryStatus status) {
        if (stopping) status = CodQueryStatus.FAILED;
        job.query.setStatus(status);
        if (status == CodQueryStatus.COMPLETED) job.query.setProgress(100);
        if (saveStatus(job.query)) admitted.remove(job.query.getElementSet(), job);
        else {
            job.query.setStatus(CodQueryStatus.FAILED);
            scheduleStatusRecovery();
        }
    }

    private void scheduleStatusRecovery() {
        if (stopping || recoveryScheduled) return;
        recoveryScheduled = true;
        // At most one scheduled retry, and at most three retained terminal records.
        statusRecovery.schedule(() -> {
            synchronized (CodImportService.this) {
                recoveryScheduled = false;
                admitted.values().removeIf(job -> job.terminal() && saveStatus(job.query));
                if (admitted.values().stream().anyMatch(ImportJob::terminal)) scheduleStatusRecovery();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private final class ImportJob implements Runnable {
        private final List<String> elements;
        private final CodQuery query;
        private ImportJob(List<String> elements, CodQuery query) {
            this.elements = elements;
            this.query = query;
        }
        private boolean terminal() {
            return query.getStatus() == CodQueryStatus.COMPLETED || query.getStatus() == CodQueryStatus.FAILED;
        }
        @Override public void run() {
            try {
                synchronized (CodImportService.this) {
                    if (stopping) throw new CancellationException("COD importer stopping");
                    query.setStatus(CodQueryStatus.RUNNING);
                    if (!saveStatus(query)) throw new IllegalStateException("Cannot persist running status");
                }
                importFromCod(elements, query);
                checkInterrupted();
                finish(this, CodQueryStatus.COMPLETED);
            } catch (Exception failure) {
                log.error("COD import {} failed ({})", query.getId(), failure.getClass().getSimpleName());
                finish(this, CodQueryStatus.FAILED);
            }
        }
    }

    private void importFromCod(List<String> elements, CodQuery query) throws IOException {
        Path csv = source.download(elements);
        long processed = 0;
        long inserted = 0;
        long updated = 0;
        long skipped = 0;
        try {
            long total;
            try (var lines = Files.lines(csv, StandardCharsets.UTF_8)) {
                total = Math.max(1, lines.count() - 1);
            }
            try (var reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
                    CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                List<CSVRecord> batch = new ArrayList<>(batchSize);
                for (CSVRecord record : parser) {
                    checkInterrupted();
                    batch.add(record);
                    if (batch.size() == batchSize) {
                        long[] counts = processBatch(batch, query, processed, total);
                        processed += batch.size(); inserted += counts[0]; updated += counts[1]; skipped += counts[2];
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    long[] counts = processBatch(batch, query, processed, total);
                    processed += batch.size(); inserted += counts[0]; updated += counts[1]; skipped += counts[2];
                }
            }
            log.info("COD import {}: processed={}, inserted={}, updated={}, skipped={}",
                    query.getId(), processed, inserted, updated, skipped);
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private long[] processBatch(List<CSVRecord> batch, CodQuery query, long processed, long total) {
        return batchTransaction.execute(status -> {
            try {
                List<String> ids = batch.stream().map(r -> r.get("file")).toList();
                Map<String, CodEntry> existing = entries.findAllByCodIdIn(ids).stream()
                        .collect(Collectors.toMap(CodEntry::getCodId, e -> e));
                List<CodEntry> toSave = new ArrayList<>(batch.size());
                long inserted = 0, updated = 0, skipped = 0;
                for (CSVRecord record : batch) {
                    checkInterrupted();
                    // Malformed CSV rows were historically skipped; preserve that behavior.
                    if (!record.isConsistent()) { skipped++; continue; }
                    String id = record.get("file");
                    CodEntry entry = existing.get(id);
                    if (entry == null) { entry = new CodEntry(); inserted++; }
                    else updated++;
                    entry.setCodId(id);
                    entry.setMineralName(record.isMapped("mineral") ? record.get("mineral") : "");
                    entry.setFormula(record.get("formula"));
                    entry.setElements(record.get("compoundsource"));
                    entry.setPublicationYear(record.get("year"));
                    entry.setAuthors(record.get("authors"));
                    entry.setJournal(record.get("journal"));
                    entry.setDoi(record.get("doi"));
                    entry.setDownloadUrl("https://www.crystallography.net/cod/" + id + ".cif");
                    entry.setLastUpdated(LocalDateTime.now());
                    toSave.add(entry);
                }
                entries.saveAll(toSave);
                query.setProgress((int) Math.min(99, (processed + batch.size()) * 100 / total));
                queries.save(query);
                entityManager.flush();
                return new long[]{inserted, updated, skipped};
            } finally {
                // Only this batch's transactional persistence context is cleared.
                entityManager.clear();
            }
        });
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("COD import interrupted");
    }

    @PreDestroy
    public void close() {
        synchronized (this) {
            if (stopping) return;
            stopping = true;
            for (ImportJob job : List.copyOf(admitted.values())) finish(job, CodQueryStatus.FAILED);
            executor.shutdownNow();
            statusRecovery.shutdownNow();
        }
        // Release admission lock before awaiting the worker's terminal status write.
        try { executor.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
