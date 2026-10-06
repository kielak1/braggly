package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record CifLimits(long maxFileBytes, int maxAtoms, int maxConcurrent,
                        int connectTimeoutMs, int readTimeoutMs) {
    public CifLimits(@Value("${cif.max-file-bytes:33554432}") long maxFileBytes,
            @Value("${cif.max-atoms:100000}") int maxAtoms,
            @Value("${cif.max-concurrent:2}") int maxConcurrent,
            @Value("${cif.connect-timeout-ms:15000}") int connectTimeoutMs,
            @Value("${cif.read-timeout-ms:60000}") int readTimeoutMs) {
        if (maxFileBytes < 1 || maxAtoms < 1 || maxConcurrent < 1
                || connectTimeoutMs < 1 || readTimeoutMs < 1) {
            throw new IllegalArgumentException("CIF limits must be positive");
        }
        this.maxFileBytes = maxFileBytes;
        this.maxAtoms = maxAtoms;
        this.maxConcurrent = maxConcurrent;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }
}
