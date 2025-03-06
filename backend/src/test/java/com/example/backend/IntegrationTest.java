package com.example.backend;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class IntegrationTest {

    private static final String BASE_URL = "http://localhost:9191/api";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = BASE_URL;
    }

    @Test
    @Order(1)
    void testHelloEndpoint() {
        given()
                .auth().basic("admin", "admin")
                .when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, world!")); // Zakładam, że zwraca taki tekst
    }

    @Test
    @Order(2)
    void testCreateUserEndpoint() {
        given()
                .auth().basic("admin", "admin")
                .contentType(ContentType.URLENC)
                .formParam("username", "testuser")
                .formParam("password", "testpass")
                .when()
                .post("/admin/create-user")
                .then()
                .statusCode(200)
                .body(equalTo("User created successfully."));
    }

    @Test
    @Order(3)
    void testDeleteUserEndpoint() {
        given()
                .auth().basic("admin", "admin")
                .queryParam("username", "testuser")
                .when()
                .delete("/admin/delete-user")
                .then()
                .statusCode(200)
                .body(equalTo("User deleted successfully."));
    }

    @Test
    @Order(4)
    void testDeleteNonExistingUser() {
        given()
                .auth().basic("admin", "admin")
                .queryParam("username", "nonexistent")
                .when()
                .delete("/admin/delete-user")
                .then()
                .statusCode(400)
                .body(equalTo("User not found."));
    }
}
