package com.example.backend.service;

import com.example.backend.model.RestrictedPath;
import com.example.backend.repository.RestrictedPathRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestrictedPathService {

    private final RestrictedPathRepository restrictedPathRepository;

    public RestrictedPathService(RestrictedPathRepository restrictedPathRepository) {
        this.restrictedPathRepository = restrictedPathRepository;
    }

    public List<RestrictedPath> getAllRestrictedPaths() {
        return restrictedPathRepository.findAll();
    }

    public RestrictedPath addPath(String path) {
        if (restrictedPathRepository.existsByPath(path)) {
            throw new IllegalArgumentException("Ścieżka już istnieje: " + path);
        }
        RestrictedPath newPath = new RestrictedPath();
        newPath.setPath(path);
        return restrictedPathRepository.save(newPath);
    }

    public void deletePath(String path) {
        restrictedPathRepository
            .findAll()
            .stream()
            .filter(p -> p.getPath().equals(path))
            .findFirst()
            .ifPresent(restrictedPathRepository::delete);
    }
}
