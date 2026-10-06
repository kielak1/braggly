package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Downloads CSV to disk; ownership of the successful temporary file passes to the importer. */
@Component
public class CodCsvSource {
    private final int connectTimeout;
    private final int readTimeout;

    public CodCsvSource(@Value("${cod.import.connect-timeout-ms:15000}") int connectTimeout,
            @Value("${cod.import.read-timeout-ms:60000}") int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public Path download(List<String> elements) throws IOException {
        String parameters = IntStream.range(0, elements.size())
                .mapToObj(i -> "el" + (i + 1) + "=" + elements.get(i)).collect(Collectors.joining("&"));
        URLConnection connection = URI.create("https://www.crystallography.net/cod/result.php?"
                + parameters + "&disp=1000000&format=csv").toURL().openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        Path csv = Files.createTempFile("cod_import", ".csv");
        try {
            try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    var writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) throw new CancellationException("COD download interrupted");
                    if (!line.trim().startsWith("#")) { writer.write(line); writer.newLine(); }
                }
            }
            return csv;
        } catch (IOException | RuntimeException failure) {
            Files.deleteIfExists(csv);
            throw failure;
        }
    }
}
