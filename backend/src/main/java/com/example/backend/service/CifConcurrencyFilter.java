package com.example.backend.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/** Retains the HTTP slot until serialization completes, including slow clients. */
@Component
public class CifConcurrencyFilter extends OncePerRequestFilter {
    private final Semaphore permits;

    public CifConcurrencyFilter(CifLimits limits) {
        permits = new Semaphore(limits.maxConcurrent());
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/cod/cif/");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!permits.tryAcquire()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"FAILED\",\"message\":\"CIF concurrency limit reached\"}");
            return;
        }
        try { chain.doFilter(request, response); }
        finally { permits.release(); }
    }
}
