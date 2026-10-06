package com.example.backend.controller;

import com.example.backend.model.CodEntry;
import com.example.backend.repository.CodEntryRepository;
import com.example.backend.repository.CodQueryRepository;
import com.example.backend.service.CodFormulaLookupService;
import com.example.backend.service.CodImportService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CodFormulaEndpointTest {
    @Test
    void reorderedFormulaReturnsExistingIdThroughEndpoint() throws Exception {
        CodEntryRepository repo = mock(CodEntryRepository.class);
        CodEntryRepository.FormulaCandidate row = mock(CodEntryRepository.FormulaCandidate.class);
        when(row.getId()).thenReturn(1L);
        when(row.getCodId()).thenReturn("synthetic-cod-id");
        when(row.getFormula()).thenReturn("- B Bi O3 -");
        when(repo.findFormulaCandidates(anyString(), eq(0L), any(Pageable.class))).thenReturn(List.of(row));
        var mvc = MockMvcBuilders.standaloneSetup(new CodController(mock(CodImportService.class),
                new CodFormulaLookupService(repo), mock(CodQueryRepository.class))).build();
        for (String formula : new String[]{"Bi B O3", "- B Bi O3 -", "Bi1 B1 O3", " Bi   B O3 "}) {
            mvc.perform(get("/api/cod/id").param("formula", formula)).andExpect(status().isOk())
                    .andExpect(content().json("[\"synthetic-cod-id\"]"));
        }
        mvc.perform(get("/api/cod/id").param("formula", "Bi B O2")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
