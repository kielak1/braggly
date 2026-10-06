package com.example.backend.service;

import com.example.backend.model.CodEntry;
import com.example.backend.repository.CodEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CodFormulaLookupService.class)
class CodFormulaLookupIntegrationTest {
    @Autowired CodEntryRepository repository;
    @Autowired CodFormulaLookupService lookup;

    private void add(String id, String formula) {
        CodEntry entry = new CodEntry();
        entry.setCodId(id);
        entry.setFormula(formula);
        repository.saveAndFlush(entry);
    }

    @Test
    void lookupUsesCompositionAndLeavesStoredTextUntouched() {
        add("match", "- B Bi O3 -");
        add("explicit", "Bi1 B1 O3.00");
        add("different", "B Bi O30");
        add("similar", "B I O3");
        add("complex", "Bi(BO3)");
        for (String query : new String[]{"Bi B O3", "- B Bi O3 -", " Bi\t B O3 ", "Bi1B1O3", "Bi B O O2"}) {
            assertThat(lookup.findCodIds(query)).containsExactlyInAnyOrder("match", "explicit");
        }
        assertThat(repository.findByCodId("match").orElseThrow().getFormula()).isEqualTo("- B Bi O3 -");
        assertThat(lookup.findCodIds("Bi B O2")).isEmpty();
    }

    @Test
    void unsupportedNotationRetainsExactLookupOnly() {
        add("complex", "Bi(BO3)");
        add("simple", "Bi B O3");
        assertThat(lookup.findCodIds("Bi(BO3)")).containsExactly("complex");
        assertThat(lookup.findCodIds("Bi(BO2)")).isEmpty();
    }

    @Test
    void lookupFindsMatchAfterFirstCandidatePage() {
        for (int i = 0; i < 505; i++) add("other-" + i, "Bi B O2");
        add("last", "- B Bi O3 -");
        assertThat(lookup.findCodIds("Bi B O3")).containsExactly("last");
    }
}
