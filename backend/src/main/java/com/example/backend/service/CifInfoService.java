package com.example.backend.service;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.Block;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

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
        System.out.println("Attempting to download CIF file with key: " + key);
        try {
            cifStream = cloudStorageService.downloadFile(key);
        } catch (Exception ex) {
            try {
                String url = "https://www.crystallography.net/cod/" + codId + ".cif";
                InputStream downloaded = new URL(url).openStream();
                cloudStorageService.uploadInputStream(key, downloaded);
                cifStream = cloudStorageService.downloadFile(key);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie udało się pobrać pliku .cif");
            }
        }

        return parseCifFile(cifStream);
    }

    private Map<String, String> parseCifFile(InputStream stream) {
        Map<String, String> result = new LinkedHashMap<>();
        try (InputStream closableStream = stream) {
            byte[] fileContent = closableStream.readAllBytes();

            CifFile cifFile = CifIO.readFromInputStream(new java.io.ByteArrayInputStream(fileContent));

            Block block = cifFile.getBlocks().get(0);
            result.put("name", getValue(block, "chemical_name_systematic"));
            result.put("formula", getValue(block, "chemical_formula_sum"));
            result.put("volume", getValue(block, "cell_volume"));
            result.put("a", getValue(block, "cell_length_a"));
            result.put("b", getValue(block, "cell_length_b"));
            result.put("c", getValue(block, "cell_length_c"));
            result.put("spaceGroup", getValue(block, "symmetry_space_group_name_H-M"));
            result.put("year", getValue(block, "journal_year"));
            result.put("author", getValue(block, "publ_author_name"));

        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas parsowania pliku CIF", e);
        }
        return result;
    }

    private String getValue(Block block, String fullTag) {

        String categoryName = fullTag.startsWith("_") ? fullTag.substring(1) : fullTag;

        Category category = block.getCategory(categoryName);
        if (category == null) {
            System.out.println("Brak kategorii: " + categoryName);
            return "";
        }

        if (!category.getColumnNames().contains("")) {
            // System.out.println("Brak kolumny '' w kategorii: " + categoryName);
            return "";
        }

        var column = category.getColumn("");
        int rowCount = column.getRowCount();
        if (rowCount == 0) {
            System.out.println("Kolumna '' istnieje, ale brak danych.");
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            sb.append(column.getStringData(i).trim());
            if (i < rowCount - 1) {
                sb.append("; ");
            }
        }
        String result = sb.toString();
        return result;
    }

}