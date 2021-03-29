package io.quarkus.reactivemessaging.http;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ReactiveMessagingHttpTest {

    @Test
    public void shouldSendAndConsumeWebSocketAndUseCustomSerializer() {
        //@formatter:off
        given()
                .body("test-message")
        .when()
                .post("/websocket-helper")
        .then()
                .statusCode(204);
        //@formatter:on

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> get("/websocket-helper").getBody().asString(), Predicate.isEqual("TEST-MESSAGE"));
    }

    @Test
    public void shouldSendAndConsumeHttpAndUseCustomSerializer() throws Exception {
        //@formatter:off
        given()
                .body("test-message")
                .when()
        .post("/http-helper")
                .then()
                .statusCode(204);
        //@formatter:on

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> get("/http-helper").getBody().asString(), Predicate.isEqual("TEST-MESSAGE"));
    }

    @AfterEach
    public void cleanUp() {
        delete("/http-helper").then().statusCode(204);
        delete("/websocket-helper").then().statusCode(204);
    }
}
