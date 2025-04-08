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
        String prompt = "Podaj wzór związku chemicznego w formacie akceptowanym przez wyszukiwarkę Crystallography Open Database (COD) "
                + "oraz poprawną nazwę chemiczną (w języku angielskim) dla substancji o nazwie '" + compoundName.trim()
                + "'. Odpowiedz wyłącznie w formacie JSON w postaci: { \"formula\": \"- A1 B2 ... NX -\", \"name\": \"nazwa\" }, "
                + "gdzie A, B, ..., N to symbole pierwiastków chemicznych, a 1, 2, ..., X to liczby atomów. Wzór musi być otoczony spacjami i znakami '-' (dokładnie: '- ' przed pierwszym atomem i ' -' po ostatnim). "
                + "Nigdy nie dawaj dwóch białych znaków po sobie. Nigdy niedawaja odstęppu pomiędzy symbolami pierwiastków i liczbami atomów. "
                + "Przed pierwszym znakiem '-' nie może być żadnych znaków ani po ostatnim znaku '-' nie może być żadnych spacji. 'name' to poprawna nazwa chemiczna substancji. Nie dodawaj żadnych komentarzy ani dodatkowego tekstu.";

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