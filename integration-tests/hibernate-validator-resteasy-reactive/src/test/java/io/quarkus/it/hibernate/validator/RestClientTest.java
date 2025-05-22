package io.quarkus.it.hibernate.validator;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RestClientTest {

    @Test
    public void fetchDefault() {
        given().get("hibernate-validator/test-rest-client")
                .then()
                .statusCode(200);
    }
}
