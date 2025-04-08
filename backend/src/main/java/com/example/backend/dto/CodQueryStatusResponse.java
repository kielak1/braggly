package com.example.backend.dto;

import java.time.LocalDateTime;

public class CodQueryStatusResponse {
    private boolean alreadyQueried;
    private boolean queryRunning;
    private boolean completed;
    private LocalDateTime lastCompleted;
    private int progress; 

    public CodQueryStatusResponse(boolean alreadyQueried, boolean queryRunning, boolean completed,
            LocalDateTime lastCompleted, int progress) {
        this.alreadyQueried = alreadyQueried;
        this.queryRunning = queryRunning;
        this.completed = completed;
        this.lastCompleted = lastCompleted;
        this.progress = progress;
    }

    // Możesz zostawić poprzedni konstruktor, jeśli jest używany gdzieś indziej
    public CodQueryStatusResponse(boolean alreadyQueried, boolean queryRunning, boolean completed,
            LocalDateTime lastCompleted) {
        this(alreadyQueried, queryRunning, completed, lastCompleted, 0);
    }

    public boolean isAlreadyQueried() {
        return alreadyQueried;
    }

    public boolean isQueryRunning() {
        return queryRunning;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getLastCompleted() {
        return lastCompleted;
    }

    public int getProgress() {
        return progress;
    }
}
