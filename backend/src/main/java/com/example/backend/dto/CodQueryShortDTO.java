package com.example.backend.dto;

public class CodQueryShortDTO {
    private String formula;
    private String requestedAt;
    private String eta;

    public CodQueryShortDTO(String formula, String requestedAt, String eta) {
        this.formula = formula;
        this.requestedAt = requestedAt;
        this.eta = eta;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }
}
