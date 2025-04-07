package com.example.backend.dto;

import java.time.LocalDateTime;

public class CodQueryStatusResponse {
    private boolean alreadyQueried;
    private boolean queryRunning;
    private boolean completed;
    private LocalDateTime lastCompleted;

    public CodQueryStatusResponse(boolean alreadyQueried, boolean queryRunning, boolean completed,
            LocalDateTime lastCompleted) {
        this.alreadyQueried = alreadyQueried;
        this.queryRunning = queryRunning;
        this.completed = completed;
        this.lastCompleted = lastCompleted;
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
}
