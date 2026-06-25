package io.quarkus.it.vertx.nativetransport;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.config.NativeTransportType;
import io.restassured.response.Response;

/**
 * Verifies that with all three native transports on the classpath (epoll, kqueue, io_uring)
 * and auto selection, the OS-appropriate transport is picked:
 * epoll on Linux, kqueue on macOS.
 */
@QuarkusTest
class AutoSelectTransportTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldAutoSelectEpollOnLinux() {
        Response response = given()
                .when().get("/transport")
                .then().statusCode(200)
                .extract().response();

        String type = response.jsonPath().getString("type");
        boolean nativeEnabled = response.jsonPath().getBoolean("nativeTransportEnabled");

        assertThat(nativeEnabled).isTrue();
        assertThat(type).isEqualTo(NativeTransportType.EPOLL.transportName);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void shouldAutoSelectKqueueOnMac() {
        Response response = given()
                .when().get("/transport")
                .then().statusCode(200)
                .extract().response();

        String type = response.jsonPath().getString("type");
        boolean nativeEnabled = response.jsonPath().getBoolean("nativeTransportEnabled");

        assertThat(nativeEnabled).isTrue();
        assertThat(type).isEqualTo(NativeTransportType.KQUEUE.transportName);
    }
}
