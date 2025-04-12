package com.example.backend.init;

import com.example.backend.repository.CodQueryRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

@Component
public class StartupCleaner {

    private final CodQueryRepository codQueryRepository;
    private final DataSource dataSource;

    public StartupCleaner(CodQueryRepository codQueryRepository, DataSource dataSource) {
        this.codQueryRepository = codQueryRepository;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        cleanIncompleteQueries();
        diagnoseDatabaseFromUrl();
        testInternalDatabaseHost();
        printEnvVariables();
        testDatabaseConnection();
        printSystemDiagnostics();
        printDatabaseVersion();
    }

    private void cleanIncompleteQueries() {
        long toDelete = codQueryRepository.countByCompletedFalse();
        codQueryRepository.deleteByCompletedFalse();
        System.out.println("[StartupCleaner] Usunięto " + toDelete + " nieukończonych zapytań z tabeli cod_query.");
    }

    private void diagnoseDatabaseFromUrl() {
        String jdbcUrl = System.getenv("DATABASE_URL");

        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            System.err.println("[StartupCleaner] Brak zmiennej środowiskowej DATABASE_URL.");
            return;
        }

        try {
            String cleanUrl = jdbcUrl.replace("jdbc:", "");
            URI uri = new URI(cleanUrl);

            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 5432;

            System.out.println("[StartupCleaner] DATABASE_URL wskazuje na host: " + host + ", port: " + port);

            InetAddress address = InetAddress.getByName(host);
            System.out.println("[StartupCleaner] Adres IP (IPv6/IPv4): " + address.getHostAddress());

            boolean reachable = address.isReachable(3000);
            System.out.println(
                    "[StartupCleaner] Czy host osiągalny (ICMP ping lub TCP echo)? " + (reachable ? "TAK" : "NIE"));

            try (Socket socket = new Socket(address, port)) {
                System.out.println("[StartupCleaner] Port " + port + " OTWARTY na hoście " + host + ".");
            } catch (Exception e) {
                System.err.println(
                        "[StartupCleaner] Port " + port + " NIEOSIĄGALNY na hoście " + host + ": " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[StartupCleaner] Błąd podczas przetwarzania DATABASE_URL: " + e.getMessage());
        }
    }

    private void testInternalDatabaseHost() {
        String internalHost = "postgres.railway.internal";
        int port = 5432;

        System.out.println("[StartupCleaner] Test wewnętrznego hosta Railway: " + internalHost + ":" + port);

        try {
            InetAddress address = InetAddress.getByName(internalHost);
            System.out.println("[StartupCleaner] [internal] Rozwiązano adres IP: " + address.getHostAddress());

            boolean reachable = address.isReachable(3000);
            System.out.println("[StartupCleaner] [internal] ICMP ping: " + (reachable ? "TAK" : "NIE"));

            try (Socket socket = new Socket(address, port)) {
                System.out.println("[StartupCleaner] [internal] Port " + port + " OTWARTY na hoście " + internalHost);
            } catch (Exception e) {
                System.err.println("[StartupCleaner] [internal] Port " + port + " ZAMKNIĘTY na hoście " + internalHost
                        + ": " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[StartupCleaner] [internal] Błąd przy sprawdzaniu: " + e.getMessage());
        }
    }

    private void printEnvVariables() {
        Map<String, String> env = System.getenv();

        String[] keys = {
                "DATABASE_USER", "DATABASE_PASSWORD",
                "ADMIN_USER", "DATABASE_URL"
        };

        System.out.println("[StartupCleaner] Wybrane zmienne środowiskowe:");
        for (String key : keys) {
            String value = env.get(key);
            if (value != null) {
                System.out.println("  " + key + " = " + value);
            } else {
                System.out.println("  " + key + " nie jest ustawiona");
            }
        }
    }

    private void testDatabaseConnection() {
        System.out.println("[StartupCleaner] Próba nawiązania połączenia z bazą danych...");
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            if (valid) {
                System.out.println("[StartupCleaner] Połączenie z bazą danych nawiązane pomyślnie.");
            } else {
                System.err.println("[StartupCleaner] Połączenie z bazą danych jest nieprawidłowe.");
            }
        } catch (Exception e) {
            System.err.println("[StartupCleaner] Błąd przy łączeniu z bazą danych: " + e.getMessage());
        }
    }

    private void printSystemDiagnostics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        System.out.println("[StartupCleaner] Parametry systemowe:");
        System.out.println("  System: " + osBean.getName() + " " + osBean.getVersion());
        System.out.println("  Architektura: " + osBean.getArch());
        System.out.println("  Rdzenie CPU (available): " + Runtime.getRuntime().availableProcessors());
        System.out.println("  Load average: " + osBean.getSystemLoadAverage());
    }

    private void printDatabaseVersion() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT version();")) {
                if (rs.next()) {
                    String version = rs.getString(1);
                    System.out.println("[StartupCleaner] Wersja bazy danych: " + version);
                }
            }
        } catch (Exception e) {
            System.err.println("[StartupCleaner] Nie udało się odczytać wersji bazy danych: " + e.getMessage());
        }
    }
}
