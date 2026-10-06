package com.example.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CodQueryStatus status = CodQueryStatus.PENDING;

    @Column(name = "progress", nullable = false)
    private int progress = 0;

    public CodQuery() {
    }

    public CodQuery(String elementSet, LocalDateTime requestedAt, boolean completed) {
        this.elementSet = elementSet;
        this.requestedAt = requestedAt;
        this.completed = completed;
        this.status = completed ? CodQueryStatus.COMPLETED : CodQueryStatus.PENDING;
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
        this.status = completed ? CodQueryStatus.COMPLETED : CodQueryStatus.PENDING;
    }

    public CodQueryStatus getStatus() {
        return status;
    }

    public void setStatus(CodQueryStatus status) {
        this.status = status;
        this.completed = status == CodQueryStatus.COMPLETED;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    /**
     * Zwraca listę pierwiastków z pola elementSet, np. ["C", "H", "N"]
     */
    public List<String> getElementsList() {
        return Arrays.asList(elementSet.split(","));
    }

    /**
     * Zwraca pierwiastki w formacie do wyszukiwania (spacja oddziela), np. "C H N"
     */
    public String getElementsAsFormula() {
        return String.join(" ", getElementsList());
    }
}
