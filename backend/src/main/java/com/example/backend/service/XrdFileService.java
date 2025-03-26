package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.model.XrdFile;
import com.example.backend.repository.XrdFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class XrdFileService {

    private final XrdFileRepository xrdFileRepository;
    private final CloudStorageService cloudStorageService;

    private static final Logger logger = Logger.getLogger(XrdFileService.class.getName());

    public XrdFileService(XrdFileRepository xrdFileRepository, CloudStorageService cloudStorageService) {
        this.xrdFileRepository = xrdFileRepository;
        this.cloudStorageService = cloudStorageService;
    }

    public XrdFile saveUxdFile(MultipartFile file, String userFilename, boolean publicVisible, User user)
            throws IOException {

        String storedFilename = UUID.randomUUID() + ".raw";

        // Wysyłka do Backblaze B2 przez CloudStorageService
        cloudStorageService.uploadFile(storedFilename, file);

        logger.info("Wysłano plik do B2: " + storedFilename + " od użytkownika ID: " + user.getId());

        XrdFile xrdFile = parseHeader(file.getInputStream());
        xrdFile.setStoredFilename(storedFilename);
        xrdFile.setOriginalFilename(file.getOriginalFilename());
        xrdFile.setUserFilename(userFilename);
        xrdFile.setUser(user);
        xrdFile.setPublicVisible(publicVisible); // <-- zmieniona metoda settera

        return xrdFileRepository.save(xrdFile);
    }

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
