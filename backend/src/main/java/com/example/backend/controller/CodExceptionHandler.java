package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/** Return COD/CIF errors directly; servlet ERROR dispatch would re-enter authentication. */
@RestControllerAdvice(assignableTypes = {CodController.class, CodCifController.class})
public class CodExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> controlledFailure(ResponseStatusException failure) {
        return ResponseEntity.status(failure.getStatusCode()).body(Map.of(
                "status", "FAILED", "message", failure.getReason() == null ? "COD/CIF request failed" : failure.getReason()));
    }
}
