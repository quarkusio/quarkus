package io.quarkus.it.vertx.nativetransport;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

/**
 * Verifies that NIO is used when {@code native-transport=disabled}, even though native transport dependencies
 * (epoll, kqueue) are on the classpath.
 */
@QuarkusTest
class NativeTransportDisabledTest {

    @Test
    void shouldUseNioWhenNativeTransportExplicitlyDisabled() {
        Response response = given()
                .when().get("/transport")
                .then().statusCode(200)
                .extract().response();

        boolean nativeEnabled = response.jsonPath().getBoolean("nativeTransportEnabled");
        String type = response.jsonPath().getString("type");

        assertThat(nativeEnabled).isFalse();
        assertThat(type).isEqualTo("nio");
    }
}
