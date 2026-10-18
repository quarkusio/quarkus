package io.quarkus.it.compose.devservices.postgres;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PostgresTest {

    @Test
    public void testConfig() {
        given()
                .when()
                .get("/postgres/name")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("my_config"));

        given()
                .when()
                .get("/postgres/port")
                .then()
                .statusCode(200)
                .body(Matchers.not(Matchers.emptyOrNullString()));
    }
}
