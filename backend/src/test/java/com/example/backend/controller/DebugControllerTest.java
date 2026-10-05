package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class DebugControllerTest {
    @Test
    void environmentDiagnosticsReturnOnlyConfigurationStatus() {
        Map<String, String> environment = Map.of(
                "DATABASE_URL", "jdbc:postgresql://synthetic-user:synthetic-password@localhost/test",
                "SERVER_PORT", "8080",
                "SERVER_SECRET", "synthetic-server-secret",
                "OPEN_AI_KEY", "synthetic-openai-credential",
                "DATABASE_PASSWORD", "synthetic-database-password");

        Map<String, String> response = new DebugController().environmentVariables(environment);

        assertThat(response).containsEntry("DATABASE_URL", "configured: true")
                .containsEntry("SERVER_PORT", "configured: true")
                .containsEntry("SERVER_SECRET", "configured: true")
                .doesNotContainKeys("OPEN_AI_KEY", "DATABASE_PASSWORD");
        assertThat(response.values()).doesNotContainAnyElementsOf(environment.values());
    }

    @Test
    void blankVariableIsNotConfigured() {
        assertThat(new DebugController().environmentVariables(Map.of("DATABASE_URL", " ")))
                .containsEntry("DATABASE_URL", "configured: false");
    }
}
