package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cod_query")
public class CodQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "element_set", columnDefinition = "text", nullable = false)
    private String elementSet; // np. "C,H,N"

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "progress", nullable = false)
    private int progress = 0;

    public CodQuery() {
    }

    public CodQuery(String elementSet, LocalDateTime requestedAt, boolean completed) {
        this.elementSet = elementSet;
        this.requestedAt = requestedAt;
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getElementSet() {
        return elementSet;
    }

    public void setElementSet(String elementSet) {
        this.elementSet = elementSet;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

}
