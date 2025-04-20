package com.example.backend.controller;

import com.example.backend.dto.OpenAiRequest;
import com.example.backend.dto.OpenAiResponse;
import com.example.backend.service.OpenAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/openai")
public class OpenAiController {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    public OpenAiController(OpenAiService openAiService, ObjectMapper objectMapper) {
        this.openAiService = openAiService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/simple")
    public ResponseEntity<String> getOpenAiResponse(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body("Brak treści zapytania.");
        }
        String response = openAiService.askOpenAi(prompt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cod")
    public ResponseEntity<?> getCodFormula(@RequestBody String compoundName) {

        String prompt = """
                Podaj wzór związku chemicznego i jego nazwę chemiczną (po angielsku) dla substancji o nazwie '%s'.
                Odpowiedz TYLKO w formacie JSON, bez żadnych dodatkowych pól ani komentarzy:
                {
                  "formula": "- Aₓ Bᵧ … Vᵢ -",
                  "name": "nazwa"
                }

                Zasady składni wzoru („formula”):
                1. Zawsze zaczyna się od "- " i kończy na " -", bez żadnych znaków przed lub po.
                2. Kolejne pary Symbol+Liczba atomów rozdziel pojedynczą spacją.
                3. Jeśli atom występuje tylko raz, pomiń cyfrę 1 (np. "O" zamiast "O1", "C" zamiast "C1").
                4. Nie stosuj dwóch spacji z rzędu ani spacji na początku lub końcu wzoru (poza wymaganymi "- " i " -").
                5. Symbole pierwiastków zaczynaj wielką literą, np. "Na", "Cl", "O", "H".

                UWAGA:
                - Nie używaj grup chemicznych takich jak NH4, NO3, SO4, CO3 itp.
                - Zawsze rozbijaj je na pojedyncze atomy pierwiastków i sumuj ich łączną liczbę w całej cząsteczce.
                - Dla NH4NO3 (azotan amonu) poprawna formuła to: "- N2 H4 O3 -"
                - Dla H2SO4 (kwas siarkowy) poprawna formuła to: "- H2 S O4 -"

                Przykłady prawidłowych odpowiedzi:

                {
                  "formula": "- H2 O -",
                  "name": "water"
                }

                {
                  "formula": "- C3 H8 O3 -",
                  "name": "glycerol"
                }

                {
                  "formula": "- N2 H4 O3 -",
                  "name": "ammonium nitrate"
                }

                {
                  "formula": "- H2 S O4 -",
                  "name": "sulfuric acid"
                }
                """.formatted(compoundName.trim());

        String response = openAiService.askOpenAi(prompt);

        try {
            JsonNode json = objectMapper.readTree(response);
            String formula = json.get("formula").asText();
            String chemicalName = json.get("name").asText(); // Pobieramy nazwę od LLM
            String cleaned = formula.replaceAll("[-]", "").trim();
            Set<String> elements = Arrays.stream(cleaned.split("\\s+"))
                    .map(s -> s.replaceAll("\\d+", ""))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return ResponseEntity.ok().body(
                    objectMapper.createObjectNode()
                            .put("formulaCOD", formula)
                            .put("queryCOD", String.join(" ", elements))
                            .put("elementCount", elements.size())
                            .put("compoundName", chemicalName)); // Używamy nazwy od LLM
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Niepoprawny format odpowiedzi AI");
        }
    }
}