package com.example.backend.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChemicalCompositionTest {
    @ParameterizedTest
    @ValueSource(strings = {"Bi B O3", "B Bi O3", "- B Bi O3 -", "  Bi\tB   O3  ",
            "Bi1 B1 O3", "BiBO3", "B1.0 Bi1 O3.00", "Bi B O O2", "Bi 1 B 1 O 3"})
    void equivalentFlatSumsHaveIdenticalComposition(String formula) {
        assertThat(ChemicalComposition.parse(formula)).isEqualTo(ChemicalComposition.parse("Bi B O3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bi B O2", "Bi2 B O3", "B I O3", "Bi B O30", "Bi2 B2 O6"})
    void differentCompositionsNeverMatch(String formula) {
        assertThat(ChemicalComposition.parse(formula)).isPresent().isNotEqualTo(ChemicalComposition.parse("Bi B O3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bi B O3 garbage", "Bi(BO3)", "CuSO4·5H2O", "Fe3+", "13C", "Xx2", "H0", "H-2", "C o", "- H2 O", ""})
    void unsupportedNotationIsNotPartiallyParsed(String formula) {
        assertThat(ChemicalComposition.parse(formula)).isEmpty();
    }

    @Test
    void symbolCaseRetainsMeaning() {
        assertThat(ChemicalComposition.parse("Co")).isNotEqualTo(ChemicalComposition.parse("C O"));
    }
}
