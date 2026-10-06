package com.example.backend.dto;

import java.time.LocalDateTime;
import com.example.backend.model.CodQueryStatus;

public class CodQueryStatusResponse {
    private boolean alreadyQueried;
    private boolean queryRunning;
    private boolean completed;
    private LocalDateTime lastCompleted;
    private int progress;
    private CodQueryStatus status;

    public CodQueryStatusResponse(boolean alreadyQueried, boolean queryRunning, boolean completed,
            LocalDateTime lastCompleted, int progress) {
        this.alreadyQueried = alreadyQueried;
        this.queryRunning = queryRunning;
        this.completed = completed;
        this.lastCompleted = lastCompleted;
        this.progress = progress;
        this.status = completed ? CodQueryStatus.COMPLETED : CodQueryStatus.PENDING;
    }

    public CodQueryStatusResponse(CodQueryStatus status, LocalDateTime requestedAt, int progress) {
        this(status == CodQueryStatus.COMPLETED,
                status == CodQueryStatus.PENDING || status == CodQueryStatus.RUNNING,
                status == CodQueryStatus.COMPLETED,
                status == CodQueryStatus.COMPLETED ? requestedAt : null, progress);
        this.status = status;
    }

    public CodQueryStatus getStatus() {
        return status;
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
