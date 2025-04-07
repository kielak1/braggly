package com.example.backend.dto;

public class CodImportResult {
    private String codId;
    private String mineralName;

    public CodImportResult(String codId, String mineralName) {
        this.codId = codId;
        this.mineralName = mineralName;
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
}
