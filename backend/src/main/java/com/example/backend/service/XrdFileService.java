// src/main/java/com/example/backend/service/XrdFileService.java
package com.example.backend.service;

import com.example.backend.dto.XrdFileResponseDTO;
import com.example.backend.model.User;
import com.example.backend.model.XrdFile;
import com.example.backend.repository.XrdFileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class XrdFileService {

    private final XrdFileRepository xrdFileRepository;
    private final CloudStorageService cloudStorageService;

    private static final Logger logger = Logger.getLogger(XrdFileService.class.getName());

    public XrdFileService(XrdFileRepository xrdFileRepository, CloudStorageService cloudStorageService) {
        this.xrdFileRepository = xrdFileRepository;
        this.cloudStorageService = cloudStorageService;
    }

    public XrdFile saveUxdFile(MultipartFile file, String userFilename, boolean isPublic, User user)
            throws IOException {
        String storedFilename = UUID.randomUUID() + ".raw";
        cloudStorageService.uploadFile(storedFilename, file);

        logger.info("Wysłano plik do B2: " + storedFilename + " od użytkownika ID: " + user.getId());

        XrdFile xrdFile = parseHeader(file.getInputStream());
        xrdFile.setStoredFilename(storedFilename);
        xrdFile.setOriginalFilename(file.getOriginalFilename());
        xrdFile.setUserFilename(userFilename);
        xrdFile.setUser(user);
        xrdFile.setPublicVisible(isPublic);

        return xrdFileRepository.save(xrdFile);
    }

    public List<XrdFileResponseDTO> getFilesForUser(User user) {
        return xrdFileRepository.findAll().stream()
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .map(XrdFileResponseDTO::from)
                .collect(Collectors.toList());
    }

    public XrdFileResponseDTO getFileForUser(Long id, User user) {
        XrdFile file = xrdFileRepository.findById(id)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono pliku lub brak dostępu"));
        return XrdFileResponseDTO.from(file);
    }

    public XrdFileResponseDTO updateFileMetadata(Long id, XrdFileResponseDTO updated, User user) {
        XrdFile file = xrdFileRepository.findById(id)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono pliku lub brak dostępu"));

        file.setUserFilename(updated.getUserFilename());
        file.setPublicVisible(updated.isPublicVisible());
        return XrdFileResponseDTO.from(xrdFileRepository.save(file));
    }

    public void deleteFile(Long id, User user) {
        XrdFile file = xrdFileRepository.findById(id)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono pliku lub brak dostępu"));

        cloudStorageService.deleteFile(file.getStoredFilename());
        xrdFileRepository.delete(file);
    }

    // 🔍 Parsowanie nagłówka UXD
    private XrdFile parseHeader(InputStream stream) throws IOException {
        XrdFile xrd = new XrdFile();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().matches("^\\d"))
                break;

            if (line.startsWith("_SAMPLE="))
                xrd.setSample(stripQuotes(line));
            else if (line.startsWith("_+SAMPLE="))
                xrd.setSampleDescription(stripQuotes(line));
            else if (line.startsWith("_SITE="))
                xrd.setSite(stripQuotes(line));
            else if (line.startsWith("_USER="))
                xrd.setInstitutionUser(stripQuotes(line));
            else if (line.startsWith("_ANODE="))
                xrd.setAnode(stripQuotes(line));
            else if (line.startsWith("_DETECTORSLIT="))
                xrd.setDetectorSlit(stripQuotes(line));
            else if (line.startsWith("_DATEMEASURED="))
                xrd.setDateMeasured(stripQuotes(line));
            else if (line.startsWith("_WL1="))
                xrd.setWavelength1(parseDouble(line));
            else if (line.startsWith("_WL2="))
                xrd.setWavelength2(parseDouble(line));
            else if (line.startsWith("_WL3="))
                xrd.setWavelength3(parseDouble(line));
            else if (line.startsWith("_STEPTIME="))
                xrd.setStepTime(parseDouble(line));
            else if (line.startsWith("_STEPSIZE="))
                xrd.setStepSize(parseDouble(line));
            else if (line.startsWith("_KV="))
                xrd.setKv(parseInt(line));
        }
        return xrd;
    }

    private String stripQuotes(String line) {
        return line.split("=", 2)[1].replace("'", "").replace("\"", "").trim();
    }

    private Double parseDouble(String line) {
        try {
            return Double.parseDouble(line.split("=", 2)[1].trim());
        } catch (Exception e) {
            logger.warning("Błąd parsowania double: " + line);
            return null;
        }
    }

    private Integer parseInt(String line) {
        try {
            return Integer.parseInt(line.split("=", 2)[1].trim());
        } catch (Exception e) {
            logger.warning("Błąd parsowania int: " + line);
            return null;
        }
    }
}
