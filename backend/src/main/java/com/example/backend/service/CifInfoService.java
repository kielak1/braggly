package com.example.backend.service;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.EmptyColumnException;
import org.rcsb.cif.ParsingException;
import org.rcsb.cif.model.Block;
import org.rcsb.cif.model.Column;
import org.rcsb.cif.model.ValueKind;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.Category;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class CifInfoService {

    private final CloudStorageService cloudStorageService;

    private final CifLimits limits;
    private final Semaphore permits;

    public CifInfoService(CloudStorageService cloudStorageService, CifLimits limits) {
        this.cloudStorageService = cloudStorageService;
        this.limits = limits;
        this.permits = new Semaphore(limits.maxConcurrent());
    }

    public Map<String, Object> getStructureInfo(String codId) {
        if (!codId.matches("[0-9]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid COD ID");
        if (!permits.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "CIF concurrency limit reached");
        try {
            String key = "cif/" + codId + ".cif";
            InputStream cached;
            try {
                cached = cloudStorageService.downloadFile(key);
            } catch (IOException missing) {
                return downloadAndCache(codId, key);
            } catch (software.amazon.awssdk.services.s3.model.S3Exception missing) {
                if (missing.statusCode() != 404) throw missing;
                return downloadAndCache(codId, key);
            }
            return parseCifFile(cached);
        } finally {
            permits.release();
        }
    }

    private Map<String, Object> downloadAndCache(String codId, String key) {
        Path file = null;
        try {
            URLConnection connection = openCodConnection(codId);
            connection.setConnectTimeout(limits.connectTimeoutMs());
            connection.setReadTimeout(limits.readTimeoutMs());
            try (InputStream raw = connection.getInputStream();
                    InputStream input = new LimitedInputStream(raw, limits.maxFileBytes())) {
                if (connection.getContentLengthLong() > limits.maxFileBytes()) throw new LimitedInputStream.SizeLimitExceededException();
                file = Files.createTempFile("braggly-cif-", ".cif");
                Files.copy(input, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // Reuse the same bounded file for parsing and SDK streaming; no second B2 GET.
            Map<String, Object> structure = parseCifFile(Files.newInputStream(file));
            cloudStorageService.uploadFile(key, file);
            return structure;
        } catch (LimitedInputStream.SizeLimitExceededException tooLarge) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CIF size limit exceeded");
        } catch (IOException failure) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie udało się pobrać pliku .cif");
        } finally {
            if (file != null) {
                try { Files.deleteIfExists(file); }
                catch (IOException failure) { throw new IllegalStateException("Cannot remove CIF temporary file", failure); }
            }
        }
    }

    URLConnection openCodConnection(String codId) throws IOException {
        return URI.create("https://www.crystallography.net/cod/" + codId + ".cif").toURL().openConnection();
    }

    private Map<String, Object> parseCifFile(InputStream stream) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (InputStream closableStream = new LimitedInputStream(stream, limits.maxFileBytes())) {
            CifFile cifFile = CifIO.readFromInputStream(closableStream);
            if (cifFile.getBlocks().isEmpty()) throw CifAtomTypeResolver.unsupported();
            long totalAtoms = cifFile.getBlocks().stream()
                    .mapToLong(b -> b.getCategory("atom_site_label").getRowCount()).sum();
            if (totalAtoms > limits.maxAtoms()) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CIF atom limit exceeded");
            }
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

        } catch (LimitedInputStream.SizeLimitExceededException tooLarge) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CIF size limit exceeded");
        } catch (EmptyColumnException | ParsingException unsupported) {
            throw CifAtomTypeResolver.unsupported();
        } catch (UncheckedIOException failure) {
            // BufferedReader.lines() in ciftools wraps read failures after its initial probe.
            if (failure.getCause() instanceof LimitedInputStream.SizeLimitExceededException) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CIF size limit exceeded");
            }
            throw new RuntimeException("Błąd podczas parsowania pliku CIF", failure);
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

        var labels = block.getCategory("atom_site_label").getColumn("");
        var types = block.getCategory("atom_site_type_symbol").getColumn("");
        var component0 = block.getCategory("atom_site_label_component_0").getColumn("");
        var fractX = block.getCategory("atom_site_fract_x").getColumn("");
        var fractY = block.getCategory("atom_site_fract_y").getColumn("");
        var fractZ = block.getCategory("atom_site_fract_z").getColumn("");

        int rowCount = labels.getRowCount();
        if (rowCount == 0 || fractX.getRowCount() != rowCount || fractY.getRowCount() != rowCount
                || fractZ.getRowCount() != rowCount
                || (types.isDefined() && types.getRowCount() != rowCount)
                || (component0.isDefined() && component0.getRowCount() != rowCount)) {
            throw CifAtomTypeResolver.unsupported();
        }
        if (rowCount > limits.maxAtoms()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CIF atom limit exceeded");
        }
        var typeDictionary = block.getCategory("atom_type_symbol").getColumn("");
        Set<String> declaredTypes = new HashSet<>();
        for (int i = 0; i < typeDictionary.getRowCount(); i++) {
            String code = optionalValue(typeDictionary, i);
            if (code != null) declaredTypes.add(code);
        }

        for (int i = 0; i < rowCount; i++) {
            Map<String, String> atom = new LinkedHashMap<>();
            String label = requiredValue(labels, i);
            atom.put("label", label);
            atom.put("element", CifAtomTypeResolver.resolve(optionalValue(types, i),
                    optionalValue(component0, i), label, declaredTypes));
            atom.put("x", requiredValue(fractX, i));
            atom.put("y", requiredValue(fractY, i));
            atom.put("z", requiredValue(fractZ, i));

      //      System.out.println("[DEBUG] Atom #" + i + ": " + atom);
            atoms.add(atom);
        }

        return atoms;
    }

    private String optionalValue(Column<?> column, int row) {
        if (!column.isDefined() || column.getValueKind(row) != ValueKind.PRESENT) return null;
        return column.getStringData(row).trim();
    }

    private String requiredValue(Column<?> column, int row) {
        String value = optionalValue(column, row);
        if (value == null || value.isBlank()) throw CifAtomTypeResolver.unsupported();
        return value;
    }

}
