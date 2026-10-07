package com.example.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class CifAtomTypeResolverTest {
    @ParameterizedTest
    @CsvSource({"C1,C", "Cl2,Cl", "B1,B", "Bi1,Bi", "S1,S", "Si2,Si", "N1,N", "Na2,Na",
            "Co1,Co", "Fe3,Fe", "O1A,O", "O(3A),O", "C(1A),C", "N(1),N", "C_a_phe_83_a_0,C", "Og1,Og"})
    void infersOnlyUnambiguousPeriodicSymbols(String label,String expected) {
        assertThat(CifAtomTypeResolver.resolve(null,null,label,Set.of())).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings={"Unknown1","Site1","CA1","CO1","CL2","Cobalt1","Xx1","Q1","FeNi1","13C1","Fe3+17","Cfoo"})
    void doesNotGuessAmbiguousLabels(String label) {
        assertThatThrownBy(() -> CifAtomTypeResolver.resolve(null,null,label,Set.of()))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
    }

    @ParameterizedTest
    @CsvSource({"Cl-,Cl", "Bi+3,Bi", "Fe3+,Fe", "O-2,O", "C,C", "Co2+,Co"})
    void explicitTypeHasPrecedenceAndSeparatesOxidationState(String type,String element) {
        assertThat(CifAtomTypeResolver.resolve(type,"N","C1",Set.of("C"))).isEqualTo(element);
    }

    @Test void separateComponentZeroPrecedesTheFreeLabelAndUsesTheTypeDictionary() {
        assertThat(CifAtomTypeResolver.resolve(null,"Fe3+","arbitrary-site",Set.of("Fe3+"))).isEqualTo("Fe");
        assertThat(CifAtomTypeResolver.resolve(null,null,"O(3A)",Set.of("C","H","O"))).isEqualTo("O");
    }

    @Test void unknownExplicitTypeAndConflictingTypeDictionaryAreNotSilentlyOverridden() {
        assertThatThrownBy(() -> CifAtomTypeResolver.resolve("dummy",null,"C1",Set.of()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> CifAtomTypeResolver.resolve(null,null,"Cl2",Set.of("C")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @ParameterizedTest
    @ValueSource(strings={"?","."})
    void missingValueMarkersCanUseTheLabelFallback(String type) {
        assertThat(CifAtomTypeResolver.resolve(type,null,"Cl2",Set.of())).isEqualTo("Cl");
    }
}
