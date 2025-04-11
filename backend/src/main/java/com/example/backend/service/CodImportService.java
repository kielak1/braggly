package com.example.backend.service;

import com.example.backend.dto.CodImportResult;
import com.example.backend.dto.CodQueryStatusResponse;
import com.example.backend.model.CodEntry;
import com.example.backend.model.CodQuery;
import com.example.backend.repository.CodEntryRepository;
import com.example.backend.repository.CodQueryRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CodImportService {

    private static final Logger log = LoggerFactory.getLogger(CodImportService.class);

    private final CodEntryRepository codEntryRepository;
    private final CodQueryRepository codQueryRepository;

    public CodImportService(CodEntryRepository codEntryRepository, CodQueryRepository codQueryRepository) {
        this.codEntryRepository = codEntryRepository;
        this.codQueryRepository = codQueryRepository;
    }

    public CodQueryStatusResponse checkAndImport(List<String> elements) {
        Set<String> requestedSet = new HashSet<>(elements);
        String normalizedKey = elements.stream().sorted().collect(Collectors.joining(","));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(240);

        List<CodQuery> completed = codQueryRepository.findRecentCompletedQueries(cutoff);
        for (CodQuery q : completed) {
            Set<String> qSet = new HashSet<>(Arrays.asList(q.getElementSet().split(",")));
            if (requestedSet.containsAll(qSet)) {
                return new CodQueryStatusResponse(true, false, true, q.getRequestedAt(), 100);
            }
        }

        List<CodQuery> pending = codQueryRepository.findRecentPendingQueries(cutoff);
        for (CodQuery q : pending) {
            if (q.getElementSet().equals(normalizedKey)) {
                return new CodQueryStatusResponse(false, true, false, null, q.getProgress());
            }
        }

        CodQuery newQuery = new CodQuery(normalizedKey, now, false);
        codQueryRepository.save(newQuery);

        new Thread(() -> {
            importFromCod(elements, newQuery);
            newQuery.setCompleted(true);
            codQueryRepository.save(newQuery);
        }).start();

        return new CodQueryStatusResponse(false, true, false, null, 0);
    }

    @Transactional
    public List<CodImportResult> importFromCod(List<String> elements, CodQuery query) {
        List<CodImportResult> results = new ArrayList<>();
        Path tempFile = null;

        Instant startAll = Instant.now();

        try {
            String baseUrl = "https://www.crystallography.net/cod/result.php";
            String queryParams = IntStream.range(0, elements.size())
                    .mapToObj(i -> "el" + (i + 1) + "=" + elements.get(i))
                    .collect(Collectors.joining("&"));

            String fullUrl = baseUrl + "?" + queryParams + "&disp=1000000&format=csv";
            log.info("Pobieranie danych z COD: {}", fullUrl);

            URL url = new URL(fullUrl);
            tempFile = Files.createTempFile("cod_import", ".csv");

            int totalLines = 0;
            Instant startDownload = Instant.now();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {

                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.trim().startsWith("#")) {
                        out.write(line);
                        out.newLine();
                        totalLines++;
                    }
                }
            }
            log.info("[TIMER] Pobieranie i zapis do pliku trwało: {} sekund",
                    Duration.between(startDownload, Instant.now()).toSeconds());
            log.info("Liczba rekordów do przetworzenia: {}", totalLines);

            Instant startProcessing = Instant.now();
            Duration dbInteractionDuration = Duration.ZERO;

            try (BufferedReader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8)) {
                CSVParser csvParser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

                Iterator<CSVRecord> iterator = csvParser.iterator();
                List<CSVRecord> batch = new ArrayList<>();
                int batchSize = 500;
                int processed = 0;

                while (iterator.hasNext()) {
                    batch.add(iterator.next());
                    if (batch.size() >= batchSize) {
                        dbInteractionDuration = dbInteractionDuration.plus(
                                processBatch(batch, results, query, processed, totalLines));
                        processed += batch.size();
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    dbInteractionDuration = dbInteractionDuration.plus(
                            processBatch(batch, results, query, processed, totalLines));
                }

                log.info("[TIMER] Przetwarzanie pliku CSV trwało: {} sekund",
                        Duration.between(startProcessing, Instant.now()).toSeconds());
                log.info("[TIMER] Łączny czas interakcji z bazą: {} sekund",
                        dbInteractionDuration.toSeconds());
            }

            log.info("[TIMER] Łączny czas importu: {} sekund", Duration.between(startAll, Instant.now()).toSeconds());
            log.info("Zakończono import z COD. Liczba rekordów: {}", results.size());

        } catch (IOException e) {
            log.error("Błąd I/O podczas pobierania lub parsowania danych z COD", e);
        } catch (IllegalArgumentException e) {
            log.error("Błąd danych wejściowych lub brak nagłówków CSV", e);
        } catch (Exception e) {
            log.error("Niespodziewany błąd podczas importu danych z COD", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.info("Usunięto plik tymczasowy: {}", tempFile);
                } catch (IOException e) {
                    log.warn("Nie udało się usunąć pliku tymczasowego: {}", tempFile);
                }
            }
        }

        return results;
    }

    private Duration processBatch(List<CSVRecord> batch, List<CodImportResult> results,
                                  CodQuery query, int processedSoFar, int totalLines) {
        Duration dbDuration = Duration.ZERO;

        List<String> codIds = batch.stream()
                .map(r -> r.get("file"))
                .collect(Collectors.toList());

        // ⏱️ Pomiar: findAllByCodIdIn
        Instant dbStart = Instant.now();
        Map<String, CodEntry> existingEntries = codEntryRepository.findAllByCodIdIn(codIds)
                .stream()
                .collect(Collectors.toMap(CodEntry::getCodId, e -> e));
        Duration findDuration = Duration.between(dbStart, Instant.now());
        dbDuration = dbDuration.plus(findDuration);
        log.info("[TIMER]   czas findAllByCodIdIn: {} ms", findDuration.toMillis());

        // Budowanie obiektów (nie wliczane do DB)
        List<CodEntry> toSave = new ArrayList<>();

        for (CSVRecord record : batch) {
            try {
                String codId = record.get("file");
                String mineral = record.isMapped("mineral") ? record.get("mineral") : "";

                CodEntry entry = existingEntries.getOrDefault(codId, new CodEntry());
                entry.setCodId(codId);
                entry.setMineralName(mineral);
                entry.setFormula(record.get("formula"));
                entry.setElements(record.get("compoundsource"));
                entry.setPublicationYear(record.get("year"));
                entry.setAuthors(record.get("authors"));
                entry.setJournal(record.get("journal"));
                entry.setDoi(record.get("doi"));
                entry.setDownloadUrl("https://www.crystallography.net/cod/" + codId + ".cif");
                entry.setLastUpdated(LocalDateTime.now());

                toSave.add(entry);
                results.add(new CodImportResult(codId, mineral));
            } catch (Exception e) {
                log.warn("Błąd podczas parsowania rekordu CSV", e);
            }
        }

        // ⏱️ Pomiar: saveAll
        dbStart = Instant.now();
        codEntryRepository.saveAll(toSave);
        Duration saveAllDuration = Duration.between(dbStart, Instant.now());
        dbDuration = dbDuration.plus(saveAllDuration);
        log.info("[TIMER]   czas saveAll: {} ms", saveAllDuration.toMillis());

        int totalSoFar = processedSoFar + batch.size();
        int progress = (int) (((double) totalSoFar / totalLines) * 100);

        // ⏱️ Pomiar: save(query)
        dbStart = Instant.now();
        codQueryRepository.save(query);
        Duration saveQueryDuration = Duration.between(dbStart, Instant.now());
        dbDuration = dbDuration.plus(saveQueryDuration);
        log.info("[TIMER]   czas save(query): {} ms", saveQueryDuration.toMillis());

        return dbDuration;
    }
}
