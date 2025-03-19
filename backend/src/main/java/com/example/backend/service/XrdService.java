// src/main/java/com/example/backend/service/XrdService.java
package com.example.backend.service;

import com.example.backend.model.Peak;
import com.example.backend.model.XrdData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class XrdService {
    private static final double WAVELENGTH = 1.54056; // Cu Kα1 w Å
    private static final Logger logger = LoggerFactory.getLogger(XrdService.class);

    public XrdData analyzeXrdFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            logger.error("Received empty or null file for XRD analysis");
            throw new IllegalArgumentException("File cannot be empty");
        }

        List<Double> angles = new ArrayList<>();
        List<Integer> intensities = new ArrayList<>();
        List<Peak> peaks;

        // Parsowanie pliku
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean dataStarted = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("_2THETACOUNTS")) {
                    dataStarted = true;
                    continue;
                }
                if (dataStarted && !line.trim().isEmpty()) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2) {
                        try {
                            double angle = Double.parseDouble(parts[0]);
                            int intensity = Integer.parseInt(parts[1]);
                            angles.add(angle);
                            intensities.add(intensity);
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid data format in line: {}", line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing XRD file: {}", file.getOriginalFilename(), e);
            throw e;
        }

        if (angles.isEmpty() || intensities.isEmpty()) {
            logger.error("No valid data found in XRD file: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("No valid XRD data found in file");
        }

        // Wykrywanie pików
        peaks = detectPeaks(angles, intensities);
        return new XrdData(angles, intensities, peaks);
    }

    private List<Peak> detectPeaks(List<Double> angles, List<Integer> intensities) {
        List<Peak> peaks = new ArrayList<>();
        for (int i = 1; i < intensities.size() - 1; i++) {
            if (intensities.get(i) > intensities.get(i - 1) &&
                intensities.get(i) > intensities.get(i + 1) &&
                intensities.get(i) > 1000) { // Próg intensywności
                double angle = angles.get(i);
                double theta = Math.toRadians(angle / 2);
                double dSpacing = WAVELENGTH / (2 * Math.sin(theta));
                peaks.add(new Peak(angle, intensities.get(i), dSpacing));
            }
        }
        logger.info("Detected {} peaks in XRD data", peaks.size());
        return peaks;
    }
}