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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            // Jeżeli zapytanie z bazy jest PODZBIOREM aktualnego zapytania — nie wystarcza
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

    public List<CodImportResult> importFromCod(List<String> elements, CodQuery query) {
        List<CodImportResult> results = new ArrayList<>();
        try {
            // (jak dotąd)
            String baseUrl = "https://www.crystallography.net/cod/result.php";
            String queryParams = IntStream.range(0, elements.size())
                    .mapToObj(i -> "el" + (i + 1) + "=" + elements.get(i))
                    .collect(Collectors.joining("&"));

            String fullUrl = baseUrl + "?" + queryParams + "&disp=1000&format=csv";
            log.info("Pobieranie danych z COD: {}", fullUrl);

            URL url = new URL(fullUrl);
            try (InputStream input = url.openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                List<String> validLines = reader.lines()
                        .filter(line -> !line.trim().startsWith("#"))
                        .collect(Collectors.toList());

                CSVParser csvParser = CSVParser.parse(
                        String.join("\n", validLines),
                        CSVFormat.DEFAULT.withFirstRecordAsHeader());

                int total = validLines.size();
                int processed = 0;

                for (CSVRecord record : csvParser) {
                    String codId = record.get("file");
                    String mineral = record.isMapped("mineral") ? record.get("mineral") : "";

                    CodEntry entry = codEntryRepository.findByCodId(codId).orElse(new CodEntry());
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

                    codEntryRepository.save(entry);
                    results.add(new CodImportResult(codId, mineral));

                    processed++;
                    if (processed % 100 == 0 || processed == total) {
                        int progress = (int) (((double) processed / total) * 100);
                        query.setProgress(progress);
                        codQueryRepository.save(query);
                    }
                }
                log.info("Zakończono import z COD. Liczba rekordów: {}", results.size());
            }

        } catch (IOException e) {
            log.error("Błąd I/O podczas pobierania lub parsowania danych z COD", e);
        } catch (IllegalArgumentException e) {
            log.error("Błąd danych wejściowych lub brak nagłówków CSV", e);
        } catch (Exception e) {
            log.error("Niespodziewany błąd podczas importu danych z COD", e);
        }

        return results;
    }

}
