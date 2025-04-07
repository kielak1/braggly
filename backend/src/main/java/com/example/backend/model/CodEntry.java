package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cod_entry")
public class CodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String codId;

    @Column(length = 256)
    private String mineralName;

    @Column(length = 257)
    private String formula;

    @Column(length = 258)
    private String elements;

    @Column(length = 259)
    private String publicationYear;

    @Column(length = 260)
    private String authors;

    @Column(length = 261)
    private String journal;

    @Column(length = 262)
    private String doi;

    @Column(length = 263)
    private String downloadUrl;

    private LocalDateTime lastUpdated;

    // Gettery i settery
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodId() {
        return codId;
    }

    public void setCodId(String codId) {
        this.codId = codId;
    }

    public String getMineralName() {
        return mineralName;
    }

    public void setMineralName(String mineralName) {
        this.mineralName = mineralName;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getElements() {
        return elements;
    }

    public void setElements(String elements) {
        this.elements = elements;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
