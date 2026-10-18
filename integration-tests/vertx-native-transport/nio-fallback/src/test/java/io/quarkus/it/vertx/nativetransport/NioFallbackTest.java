package io.quarkus.it.vertx.nativetransport;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
class NioFallbackTest {

    @Test
    void shouldFallBackToNioWhenNoNativeTransportAvailable() {
        Response response = given()
                .when().get("/transport")
                .then().statusCode(200)
                .extract().response();

        String type = response.jsonPath().getString("type");
        boolean nativeEnabled = response.jsonPath().getBoolean("nativeTransportEnabled");

        assertThat(nativeEnabled).isFalse();
        assertThat(type).isEqualTo("nio");
    }
}
