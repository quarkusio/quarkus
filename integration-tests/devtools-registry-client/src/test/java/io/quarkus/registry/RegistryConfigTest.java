package io.quarkus.registry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RegistryConfigTest {

    @Test
    void should_return_config() {
        given()
                .when().get("/config")
                .then()
                .statusCode(200)
                .body("maven.repository.id", is("registry.quarkus.io"));

    }

}
