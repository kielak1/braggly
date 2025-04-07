package com.example.backend.service;

import com.example.backend.dto.CodImportResult;
import com.example.backend.model.CodEntry;
import com.example.backend.repository.CodEntryRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CodImportService {

    private static final Logger log = LoggerFactory.getLogger(CodImportService.class);
    private final CodEntryRepository codEntryRepository;

    public CodImportService(CodEntryRepository codEntryRepository) {
        this.codEntryRepository = codEntryRepository;
    }

    public List<CodImportResult> importFromCod(List<String> elements) {
        List<CodImportResult> results = new ArrayList<>();

        try {
            String baseUrl = "https://www.crystallography.net/cod/result.php";
            String queryParams = IntStream.range(0, elements.size())
                    .mapToObj(i -> "el" + (i + 1) + "=" + elements.get(i))
                    .collect(Collectors.joining("&"));

            String fullUrl = baseUrl + "?" + queryParams + "&disp=1000&format=csv";
            log.info("Pobieranie danych z COD: {}", fullUrl);

            URL url = new URL(fullUrl);
            try (
                    InputStream input = url.openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                List<String> validLines = reader.lines()
                        .filter(line -> !line.trim().startsWith("#"))
                        .collect(Collectors.toList());

                CSVParser csvParser = CSVParser.parse(
                        String.join("\n", validLines),
                        CSVFormat.DEFAULT.withFirstRecordAsHeader());

                for (CSVRecord record : csvParser) {
                    String codId = record.get("file"); // poprawne pole!
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
