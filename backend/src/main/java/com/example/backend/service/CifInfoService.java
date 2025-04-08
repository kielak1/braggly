package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class CifInfoService {

    private final CloudStorageService cloudStorageService;

    @Value("${cloud.b2.bucket}")
    private String bucketName;

    public CifInfoService(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
    }

    public Map<String, String> getStructureInfo(String codId) {
        String key = "cif/" + codId + ".cif";

        InputStream cifStream;

        try {
            // Próbujemy pobrać z B2
            cifStream = cloudStorageService.downloadFile(key);
        } catch (Exception ex) {
            // Nie znaleziono – pobieramy z COD
            try {
                String url = "https://www.crystallography.net/cod/" + codId + ".cif";
                InputStream downloaded = new URL(url).openStream();

                // Upload do B2
                cloudStorageService.uploadInputStream(key, downloaded);

                // Ponowne pobranie po zapisie
                cifStream = cloudStorageService.downloadFile(key);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie udało się pobrać pliku .cif");
            }
        }

        return parseCifFile(cifStream);
    }

    private Map<String, String> parseCifFile(InputStream stream) {
        Map<String, String> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("_chemical_name_systematic")) {
                    result.put("name", parseMultilineOrInline(reader, line));
                } else if (line.startsWith("_chemical_formula_sum")) {
                    result.put("formula", parseMultilineOrInline(reader, line));
                } else if (line.startsWith("_cell_volume")) {
                    result.put("volume", line.split("\\s+", 2)[1]);
                } else if (line.startsWith("_cell_length_a")) {
                    result.put("a", line.split("\\s+", 2)[1]);
                } else if (line.startsWith("_cell_length_b")) {
                    result.put("b", line.split("\\s+", 2)[1]);
                } else if (line.startsWith("_cell_length_c")) {
                    result.put("c", line.split("\\s+", 2)[1]);
                } else if (line.startsWith("_symmetry_space_group_name_H-M")) {
                    result.put("spaceGroup", parseMultilineOrInline(reader, line));
                } else if (line.startsWith("_journal_year")) {
                    result.put("year", line.split("\\s+", 2)[1]);
                } else if (line.startsWith("_publ_author_name")) {
                    result.put("author", parseMultilineOrInline(reader, line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas parsowania pliku CIF", e);
        }

        return result;
    }

    private String parseMultilineOrInline(BufferedReader reader, String currentLine) throws IOException {
        String[] parts = currentLine.split("\\s+", 2);
        if (parts.length > 1 && !parts[1].equals(";")) {
            return parts[1].replace("'", "").trim();
        } else {
            // Parsujemy kolejne linie do momentu kolejnego ;
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(";")) {
                    break;
                }
                sb.append(line.trim()).append(" ");
            }
            return sb.toString().trim();
        }
    }
}
