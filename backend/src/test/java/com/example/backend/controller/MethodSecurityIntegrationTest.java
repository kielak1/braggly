package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.*;
import com.example.backend.service.*;
import com.example.backend.dto.XrdFileResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real HTTP, JWT authentication, controllers and PostgreSQL writes. External APIs are mocked. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MethodSecurityIntegrationTest {
    @Autowired TestRestTemplate client;
    @Autowired ObjectMapper mapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired CreditPackageRepository packages;
    @Autowired RestrictedPathRepository paths;
    @Autowired ParametersBoolRepository parameters;
    @Autowired JdbcTemplate jdbc;
    @MockBean CloudStorageService storage;
    @MockBean OpenAiService openAi;
    @MockBean XrdFileService xrdFiles;
    @MockBean XrdService xrd;

    private String userToken;
    private String adminToken;
    private User target;
    private CreditPackage creditPackage;
    private String existingPath;
    private String newPath;
    private String parameterName;
    private String suffix;

    @BeforeEach void seedLocalDatabaseAndLogin() throws Exception {
        suffix = UUID.randomUUID().toString();
        User ordinary = userService.createUser("security-user-" + suffix, "synthetic-only-password", User.Role.USER);
        User administrator = userService.createUser("security-admin-" + suffix, "synthetic-only-password", User.Role.ADMIN);
        target = userService.createUser("security-target-" + suffix, "synthetic-only-password", User.Role.USER);
        userToken = login(ordinary.getUsername());
        adminToken = login(administrator.getUsername());
        creditPackage = new CreditPackage();
        creditPackage.setCredits(25);
        creditPackage.setPriceInCents(100);
        creditPackage = packages.saveAndFlush(creditPackage);
        existingPath = "/synthetic-existing-" + suffix;
        newPath = "/synthetic-new-" + suffix;
        RestrictedPath path = new RestrictedPath();
        path.setPath(existingPath);
        paths.saveAndFlush(path);
        parameterName = "synthetic-parameter-" + suffix;
        ParametersBool parameter = new ParametersBool();
        parameter.setName(parameterName);
        parameter.setValue(true);
        parameters.saveAndFlush(parameter);
    }

    private String login(String username) throws Exception {
        var response = client.postForEntity("/api/auth/login",
                Map.of("username", username, "password", "synthetic-only-password"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(response.getBody()).get("token").asText();
    }

    private ResponseEntity<String> request(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return client.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    /** Compare every column of every table affected by the administrative operations. */
    private Map<String, List<String>> databaseState() {
        Map<String, List<String>> state = new TreeMap<>();
        for (String table : List.of("users", "user_credits", "credit_package", "credit_purchase_history",
                "credit_usage_history", "restricted_paths", "parameters_bool")) {
            state.put(table, jdbc.queryForList("select row_to_json(t)::text from " + table + " t order by row_to_json(t)::text", String.class));
        }
        return state;
    }

    enum AdminMutation { CREATE_PACKAGE, DELETE_PACKAGE, ASSIGN_CREDITS, CREATE_PATH, DELETE_PATH }

    private ResponseEntity<String> mutate(AdminMutation operation, String token) {
        return switch (operation) {
            case CREATE_PACKAGE -> request(HttpMethod.POST, "/credits/packages", Map.of("credits", 51, "priceInCents", 200), token);
            case DELETE_PACKAGE -> request(HttpMethod.DELETE, "/credits/packages/" + creditPackage.getId(), null, token);
            case ASSIGN_CREDITS -> request(HttpMethod.POST, "/credits/assign?userId=" + target.getId() + "&packageId=" + creditPackage.getId(), null, token);
            case CREATE_PATH -> request(HttpMethod.POST, "/parameters/restricted-paths?path=" + newPath, null, token);
            case DELETE_PATH -> request(HttpMethod.DELETE, "/parameters/restricted-paths?path=" + existingPath, null, token);
        };
    }

    @ParameterizedTest @EnumSource(AdminMutation.class)
    void userCannotInvokeAnyAnnotatedAdminMutationOrChangeData(AdminMutation operation) {
        var before = databaseState();
        var response = mutate(operation, userToken);
        boolean unchanged = before.equals(databaseState());
        System.out.println("METHOD_SECURITY_PROBE operation=" + operation + " status=" + response.getStatusCode().value() + " dataChanged=" + !unchanged);
        assertAll(() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN),
                () -> assertThat(unchanged).as("database rows unchanged").isTrue());
    }

    @ParameterizedTest @EnumSource(AdminMutation.class)
    void adminCanInvokeEveryAnnotatedAdminMutation(AdminMutation operation) {
        var before = databaseState();
        assertThat(mutate(operation, adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(before.equals(databaseState())).as("authorized operation changes database rows").isFalse();
    }

    @ParameterizedTest @EnumSource(AdminMutation.class)
    void anonymousCannotInvokeAnyAnnotatedAdminMutationOrChangeData(AdminMutation operation) {
        var before = databaseState();
        assertThat(mutate(operation, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(before.equals(databaseState())).isTrue();
    }

    enum ManualMutation { CREATE_USER, DELETE_USER, SET_ROLE, SET_PASSWORD, DELETE_PARAMETER, UPDATE_PARAMETER }

    private ResponseEntity<String> mutateManual(ManualMutation operation, String token) {
        return switch (operation) {
            case CREATE_USER -> request(HttpMethod.POST, "/api/admin/create-user", Map.of("username", "synthetic-created-" + suffix, "password", "synthetic-new-password"), token);
            case DELETE_USER -> request(HttpMethod.DELETE, "/api/admin/delete-user?username=" + target.getUsername(), null, token);
            case SET_ROLE -> request(HttpMethod.PUT, "/api/admin/set-role?userId=" + target.getId() + "&role=ADMIN", null, token);
            case SET_PASSWORD -> request(HttpMethod.PUT, "/api/admin/set-password?userId=" + target.getId() + "&newPassword=synthetic-new-password", null, token);
            case DELETE_PARAMETER -> request(HttpMethod.DELETE, "/parameters/delete?name=" + parameterName, null, token);
            case UPDATE_PARAMETER -> request(HttpMethod.PUT, "/parameters/update?name=" + parameterName + "&value=false", null, token);
        };
    }

    @ParameterizedTest @EnumSource(ManualMutation.class)
    void existingManualAdminChecksRejectUserWithoutChangingData(ManualMutation operation) {
        var before = databaseState();
        assertThat(mutateManual(operation, userToken).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(before.equals(databaseState())).isTrue();
    }

    @ParameterizedTest @EnumSource(ManualMutation.class)
    void existingManualAdminChecksStillAllowAdmin(ManualMutation operation) {
        var before = databaseState();
        assertThat(mutateManual(operation, adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(before.equals(databaseState())).isFalse();
    }

    @Test void authenticatedRestrictedPathReadStillWorksAndAnonymousIsRejected() {
        assertThat(request(HttpMethod.GET, "/parameters/restricted-paths", null, userToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(HttpMethod.GET, "/parameters/restricted-paths", null, adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(HttpMethod.GET, "/parameters/restricted-paths", null, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest @ValueSource(strings = {"USER", "ADMIN"})
    void allSixXrdRoleAnnotationsStillPermitBothSupportedRoles(String role) throws Exception {
        String token = role.equals("USER") ? userToken : adminToken;
        when(xrdFiles.getFilesForUser(any())).thenReturn(List.of());
        when(xrdFiles.getFileForUser(eq(1L), any())).thenReturn(new XrdFileResponseDTO());
        when(xrdFiles.updateFileMetadata(eq(1L), any(), any())).thenReturn(new XrdFileResponseDTO());
        when(xrdFiles.saveUxdFile(any(), anyString(), anyBoolean(), any())).thenAnswer(invocation -> {
            XrdFile file = new XrdFile();
            file.setId(1L);
            file.setUser(invocation.getArgument(3));
            return file;
        });
        assertThat(request(HttpMethod.GET, "/api/xrd/files", null, token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(HttpMethod.GET, "/api/xrd/files/1", null, token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(HttpMethod.PUT, "/api/xrd/files/1", Map.of("userFilename", "synthetic"), token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(HttpMethod.DELETE, "/api/xrd/files/1", null, token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        var parts = new LinkedMultiValueMap<String,Object>();
        parts.add("file", new ByteArrayResource("synthetic UXD".getBytes()) { @Override public String getFilename() { return "synthetic.uxd"; } });
        parts.add("name", "synthetic");
        parts.add("publicVisible", "false");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        assertThat(client.exchange("/api/xrd/analyze", HttpMethod.POST, new HttpEntity<>(parts, headers), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(client.exchange("/api/xrd/upload", HttpMethod.POST, new HttpEntity<>(parts, headers), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(xrdFiles).deleteFile(eq(1L), any());
    }

    @Test void anonymousCannotInvokeAnnotatedXrdOperationsIncludingPublicAnalyzeMatcher() {
        for (String path : List.of("/api/xrd/files", "/api/xrd/files/1")) {
            assertThat(request(HttpMethod.GET, path, null, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(request(HttpMethod.PUT, "/api/xrd/files/1", Map.of("userFilename", "synthetic"), null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(request(HttpMethod.DELETE, "/api/xrd/files/1", null, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var parts = new LinkedMultiValueMap<String,Object>();
        parts.add("file", new ByteArrayResource("synthetic UXD".getBytes()) { @Override public String getFilename() { return "synthetic.uxd"; } });
        parts.add("name", "synthetic");
        parts.add("publicVisible", "false");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        for (String path : List.of("/api/xrd/analyze", "/api/xrd/upload")) {
            assertThat(client.exchange(path, HttpMethod.POST, new HttpEntity<>(parts, headers), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        verifyNoInteractions(xrd, xrdFiles, storage);
    }

    @Test void normalUserIdentityDatabaseHealthAndMockedOpenAiRemainAvailable() throws Exception {
        var identity = request(HttpMethod.GET, "/api/whoami", null, userToken);
        assertThat(identity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(identity.getBody()).get("role").asText()).isEqualTo("USER");
        var health = request(HttpMethod.GET, "/actuator/health", null, adminToken);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(health.getBody()).get("status").asText()).isEqualTo("UP");
        assertThat(mapper.readTree(health.getBody()).path("components").path("db").path("status").asText()).isEqualTo("UP");
        when(openAi.askOpenAi("synthetic hello")).thenReturn("synthetic response");
        var answer = request(HttpMethod.POST, "/openai/simple", Map.of("prompt", "synthetic hello"), userToken);
        assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(answer.getBody()).isEqualTo("synthetic response");
        verify(openAi).askOpenAi("synthetic hello");
    }

    @Test void publicXrdReadRemainsPublic() {
        when(xrdFiles.getPublicFiles()).thenReturn(List.of());
        assertThat(client.getForEntity("/api/xrd/public-files", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
