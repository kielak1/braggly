package com.example.backend.controller;

import com.example.backend.config.SecurityConfig;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.JwtUtil;
import com.example.backend.service.CifInfoService;
import com.example.backend.service.CifLimits;
import com.example.backend.service.CloudStorageService;
import com.example.backend.service.UserService;
import com.example.backend.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

/** Real servlet error dispatch is essential: standalone MockMvc would miss the false 403. */
@SpringBootTest(classes = Cif403ContractIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=c3ludGhldGljLW9ubHktdGVzdC1rZXktMzItYnl0ZXMh",
                "spring.security.user.name=synthetic-admin",
                "spring.security.user.password=unused-synthetic-password",
                "logging.file.name=",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
class Cif403ContractIntegrationTest {
    @Configuration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class,
            CodCifController.class, CodExceptionHandler.class, CifInfoService.class, CifLimits.class,
            AdminController.class, SyntheticFailureController.class})
    static class TestApplication {}

    @RestController
    static class SyntheticFailureController {
        @GetMapping("/protected-synthetic-failure")
        String fail() { throw new IllegalStateException("synthetic internal detail must stay private"); }
    }

    @Autowired TestRestTemplate client;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper mapper;
    @MockBean CloudStorageService storage;
    @MockBean UserDetailsService users;
    @MockBean UserService userService;

    @BeforeEach void authenticateSyntheticUser() {
        when(users.loadUserByUsername("synthetic-user"))
                .thenReturn(new User("synthetic-user","unused-synthetic-password",User.Role.USER));
    }

    private HttpEntity<Void> authenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt.generateToken("synthetic-user"));
        return new HttpEntity<>(headers);
    }

    private static byte[] cif(boolean includeType) {
        return ("data_synthetic\nloop_\n_atom_site_label\n"
                + (includeType ? "_atom_site_type_symbol\n" : "")
                + "_atom_site_fract_x\n_atom_site_fract_y\n_atom_site_fract_z\n"
                + (includeType ? "C1 C " : "Unknown1 ") + "0.1 0.2 0.3\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test void unsupportedCifIsNotAnAuthenticationFailureAndOtherCifsRemainAccessible() throws Exception {
        when(storage.downloadFile("cif/123.cif")).thenReturn(new ByteArrayInputStream(cif(false)));
        when(storage.downloadFile("cif/456.cif")).thenReturn(new ByteArrayInputStream(cif(true)));
        var request = authenticated();

        var unsupported = client.exchange("/api/cod/cif/123", HttpMethod.GET, request, String.class);
        assertThat(unsupported.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(unsupported.getBody()).contains("FAILED", "CIF atom data is not supported")
                .doesNotContain("EmptyColumnException", "stackTrace");

        var supported = client.exchange("/api/cod/cif/456", HttpMethod.GET, request, String.class);
        assertThat(supported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(supported.getBody()).contains("\"element\":\"C\"");
    }

    @Test void anonymousRequestsStillRequireAuthentication() {
        assertThat(client.getForEntity("/api/cod/cif/456", String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(storage);
    }

    @Test void directErrorPathIsNotMadePublicByPermittingOnlyErrorDispatch() {
        assertThat(client.getForEntity("/error", String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test void unrelatedInternalFailureIs500WithoutExposingExceptionDetails() {
        var response=client.exchange("/protected-synthetic-failure?trace=true&message=true",
                HttpMethod.GET,authenticated(),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).doesNotContain("synthetic internal detail", "IllegalStateException",
                "stackTrace", "\"trace\"", "SyntheticFailureController");
    }

    @Test void userWithoutAdminRoleStillGets403FromTheRealAdminController() {
        var response=client.exchange("/api/admin/list-user",HttpMethod.GET,authenticated(),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(userService);
    }

    @Test void adminStillCanReadTheProtectedAdminEndpoint() {
        when(users.loadUserByUsername("synthetic-user"))
                .thenReturn(new User("synthetic-user","unused-synthetic-password",User.Role.ADMIN));
        when(userService.getAllUsers()).thenReturn(List.of());
        assertThat(client.exchange("/api/admin/list-user",HttpMethod.GET,authenticated(),String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void unrelatedCifStorageFailureRemains500RatherThanBeingClassifiedAsUnsupportedData() throws Exception {
        when(storage.downloadFile("cif/456.cif")).thenThrow(new IllegalStateException("synthetic storage failure"));
        var response=client.exchange("/api/cod/cif/456",HttpMethod.GET,authenticated(),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).doesNotContain("synthetic storage failure", "IllegalStateException", "\"trace\"");
    }

    @ParameterizedTest
    @CsvSource({"7209481,5", "1550148,66", "7105573,20"})
    void realCodAtomSiteFixturesReturn200WithoutNetworkAccess(String codId,int atomCount) throws Exception {
        byte[] bytes;
        try(var fixture=getClass().getResourceAsStream("/cif/"+codId+".cif")) {
            assertThat(fixture).isNotNull();bytes=fixture.readAllBytes();
        }
        when(storage.downloadFile("cif/"+codId+".cif")).thenReturn(new ByteArrayInputStream(bytes));
        var response=client.exchange("/api/cod/cif/"+codId,HttpMethod.GET,authenticated(),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var atoms=mapper.readTree(response.getBody()).get("atoms");
        assertThat(atoms.size()).isEqualTo(atomCount);
        List<String> elements=new java.util.ArrayList<>();
        atoms.forEach(atom -> elements.add(atom.get("element").asText()));
        switch(codId) {
            case "7209481" -> assertThat(elements).containsExactly("O","B","O","Bi","O");
            case "1550148" -> assertThat(elements).containsOnly("C","H","O").contains("C","H","O");
            case "7105573" -> assertThat(elements).containsOnly("C","H","N","O").contains("C","H","N","O");
        }
    }

    @Test void labelsOnlyCifAndSeparateComponentZeroAreReadThroughTheEndpoint() throws Exception {
        String header="data_synthetic\nloop_\n_atom_site_label\n_atom_site_fract_x\n_atom_site_fract_y\n_atom_site_fract_z\n";
        String text=header+"Cl2 0 0 0\nBi1 0 0 0\nFe3 0 0 0\nO1A 0 0 0\nCo1 0 0 0\nNa1 0 0 0\nSi1 0 0 0\n";
        when(storage.downloadFile("cif/123.cif")).thenReturn(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        var response=client.exchange("/api/cod/cif/123",HttpMethod.GET,authenticated(),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> elements=new java.util.ArrayList<>();
        mapper.readTree(response.getBody()).get("atoms").forEach(atom -> elements.add(atom.get("element").asText()));
        assertThat(elements).containsExactly("Cl","Bi","Fe","O","Co","Na","Si");

        String component="data_synthetic\nloop_\n_atom_site_label\n_atom_site_label_component_0\n"
                + "_atom_site_fract_x\n_atom_site_fract_y\n_atom_site_fract_z\narbitrary-site Fe3+ 0 0 0\n"
                + "loop_\n_atom_type_symbol\nFe3+\n";
        when(storage.downloadFile("cif/456.cif")).thenReturn(new ByteArrayInputStream(component.getBytes(StandardCharsets.UTF_8)));
        var second=client.exchange("/api/cod/cif/456",HttpMethod.GET,authenticated(),String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(second.getBody()).get("atoms").get(0).get("element").asText()).isEqualTo("Fe");
    }
}
