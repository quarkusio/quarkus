package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class TcpTest {

    @Test
    public void testTcpClientServer() {
        Response response = given().body("ping")
                .post("/vertx-test/tcp")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo("pong");

        response = given().body("pong")
                .post("/vertx-test/tcp")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo("ping");
    }

    @Test
    public void testSSL() {
        Response response = given().body("ping")
                .post("/vertx-test/tcp/ssl")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo("pong");

        response = given().body("pong")
                .post("/vertx-test/tcp/ssl")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo("ping");
    }
}
