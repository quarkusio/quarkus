package io.quarkus.it.vertx.nativetransport;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.config.NativeTransportType;
import io.restassured.response.Response;

@QuarkusTest
@ExtendWith(IoUringAvailableCondition.class)
class IoUringTransportTest {

    @Test
    void shouldUseIoUringTransport() {
        Response response = given()
                .when().get("/transport")
                .then().statusCode(200)
                .extract().response();

        String type = response.jsonPath().getString("type");
        boolean nativeEnabled = response.jsonPath().getBoolean("nativeTransportEnabled");

        assertThat(nativeEnabled).isTrue();
        assertThat(type).isEqualTo(NativeTransportType.IO_URING.transportName);
    }
}
