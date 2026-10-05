package com.example.backend;

import com.example.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;

import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "logging.file.name="
})
@MockBean({DataSource.class, CodQueryRepository.class, CodEntryRepository.class,
        CreditPackageRepository.class, CreditPurchaseHistoryRepository.class,
        CreditUsageHistoryRepository.class, ParametersBoolRepository.class,
        RestrictedPathRepository.class, UserCreditsRepository.class,
        UserRepository.class, XrdFileRepository.class})
@ExtendWith(OutputCaptureExtension.class)
class StartupSafetyIntegrationTest {
    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private TestRestTemplate client;

    @Test
    void applicationStartsAndServesDiagnosticsWithoutExposingCredentials(CapturedOutput output) {
        assertThat(context.isActive()).isTrue();
        assertThat(client.getForEntity("/debug/java", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(output).contains("OPEN_AI_KEY configured: true")
                .doesNotContain("synthetic-openai-credential", "synthetic-database-password",
                        "synthetic-user:synthetic-password", "synthetic-admin-password");

        String diagnostics = client.getForObject("/debug/env", String.class);
        assertThat(diagnostics).contains("configured: true")
                .doesNotContain("synthetic-password", "jdbc:postgresql");
    }
}
