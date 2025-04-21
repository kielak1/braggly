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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CifInfoService {

    private final CloudStorageService cloudStorageService;

    @Value("${cloud.b2.bucket}")
    private String bucketName;

    public CifInfoService(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
    }

    public Map<String, Object> getStructureInfo(String codId) {
        String key = "cif/" + codId + ".cif";
        InputStream cifStream;
        System.out.println("[DEBUG] Próba pobrania pliku CIF o kluczu: " + key);
        try {
            cifStream = cloudStorageService.downloadFile(key);
            System.out.println("[DEBUG] Plik CIF pobrany z Cloud Storage.");
        } catch (Exception ex) {
            System.out.println("[DEBUG] Brak pliku CIF w Cloud Storage, próba pobrania z crystallography.net.");
            try {
                String url = "https://www.crystallography.net/cod/" + codId + ".cif";
                InputStream downloaded = new URL(url).openStream();
                cloudStorageService.uploadInputStream(key, downloaded);
                cifStream = cloudStorageService.downloadFile(key);
                System.out.println("[DEBUG] Plik CIF pobrany i zapisany w Cloud Storage.");
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie udało się pobrać pliku .cif");
            }
        }

        return parseCifFile(cifStream);
    }

    private Map<String, Object> parseCifFile(InputStream stream) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (InputStream closableStream = stream) {
            byte[] fileContent = closableStream.readAllBytes();

            // DODANY DEBUG: Wyświetlenie całego pliku CIF
            String cifText = new String(fileContent);
        //    System.out.println("[DEBUG] CAŁY PLIK CIF:\n" + cifText);

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

            List<Map<String, String>> atoms = parseAtoms(block);
            result.put("atoms", atoms);

        //    System.out.println("[DEBUG] Liczba atomów wyodrębnionych: " + atoms.size());
            // if (!atoms.isEmpty()) {
            //     System.out.println("[DEBUG] Pierwszy atom: " + atoms.get(0));
            // }

        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas parsowania pliku CIF", e);
        }
        return result;
    }

    private String getValue(Block block, String fullTag) {
        String categoryName = fullTag.startsWith("_") ? fullTag.substring(1) : fullTag;

        Category category = block.getCategory(categoryName);
        if (category == null) {
            System.out.println("[DEBUG] Brak kategorii: " + categoryName);
            return "";
        }

        if (!category.getColumnNames().contains("")) {
            System.out.println("[DEBUG] Brak kolumny '' w kategorii: " + categoryName);
            return "";
        }

        var column = category.getColumn("");
        int rowCount = column.getRowCount();
        if (rowCount == 0) {
            System.out.println("[DEBUG] Kolumna '' istnieje, ale brak danych w kategorii: " + categoryName);
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            sb.append(column.getStringData(i).trim());
            if (i < rowCount - 1) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    private List<Map<String, String>> parseAtoms(Block block) {
        List<Map<String, String>> atoms = new ArrayList<>();

        var labelsCategory = block.getCategory("atom_site_label");
        var typesCategory = block.getCategory("atom_site_type_symbol");
        var xCategory = block.getCategory("atom_site_fract_x");
        var yCategory = block.getCategory("atom_site_fract_y");
        var zCategory = block.getCategory("atom_site_fract_z");

        if (labelsCategory == null || typesCategory == null || xCategory == null || yCategory == null
                || zCategory == null) {
            System.out.println("[DEBUG] Brakuje jednej z wymaganych kategorii.");
            return atoms;
        }

        var labels = labelsCategory.getColumn("");
        var types = typesCategory.getColumn("");
        var fractX = xCategory.getColumn("");
        var fractY = yCategory.getColumn("");
        var fractZ = zCategory.getColumn("");

        int rowCount = labels.getRowCount();
        System.out.println("[DEBUG] atomCount: " + rowCount);

        for (int i = 0; i < rowCount; i++) {
            Map<String, String> atom = new LinkedHashMap<>();
            atom.put("label", labels.getStringData(i).trim());
            atom.put("element", types.getStringData(i).trim());
            atom.put("x", fractX.getStringData(i).trim());
            atom.put("y", fractY.getStringData(i).trim());
            atom.put("z", fractZ.getStringData(i).trim());

      //      System.out.println("[DEBUG] Atom #" + i + ": " + atom);
            atoms.add(atom);
        }

        return atoms;
    }

}
