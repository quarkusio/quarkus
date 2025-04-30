package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class JsonSubTypesResourceTest {

    @Test
    void test() {
        Response response = given()
                .accept(ContentType.JSON)
                .when()
                .get("jsonSubTypes")
                .then()
                .statusCode(200)
                .extract().response();
        assertThat(response.jsonPath().getString("mammals[0].color")).isEqualTo("white");
        assertThat(response.jsonPath().getString("mammals[1].continent")).isEqualTo("africa");
        assertThat(response.jsonPath().getString("mammals[1].horn_length")).isEqualTo("10");
    }
}
