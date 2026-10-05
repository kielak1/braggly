package com.example.backend.init;

import com.example.backend.repository.CodQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class StartupCleanerTest {
    private final StartupCleaner cleaner = new StartupCleaner(
            mock(CodQueryRepository.class), mock(DataSource.class));

    @Test
    void diagnosticsReportPresenceWithoutPrintingValues(CapturedOutput output) {
        Map<String, String> environment = Map.of(
                "OPEN_AI_KEY", "synthetic-openai-credential",
                "DATABASE_PASSWORD", "synthetic-database-password",
                "DATABASE_URL", "jdbc:postgresql://synthetic-user:synthetic-password@localhost/test",
                "OPENAI_MODEL", "synthetic-model");

        cleaner.printEnvVariables(environment);

        assertThat(output).contains("OPEN_AI_KEY configured: true",
                "DATABASE_PASSWORD configured: true", "DATABASE_URL configured: true");
        environment.values().forEach(value -> assertThat(output).doesNotContain(value));
    }

    @Test
    void absentAndBlankCredentialsAreNotConfigured(CapturedOutput output) {
        cleaner.printEnvVariables(Map.of("OPEN_AI_KEY", "   "));

        assertThat(output).contains("OPEN_AI_KEY configured: false",
                "DATABASE_PASSWORD configured: false");
    }
}
