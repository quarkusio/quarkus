package io.quarkus.virtual.vertx;

import static org.hamcrest.Matchers.is;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Test
    void testOneWay() {
        RestAssured.get("/one-way").then()
                .assertThat().statusCode(204);

        Awaitility.await().untilAsserted(() -> {
            RestAssured.get("/one-way-verify").then()
                    .assertThat().statusCode(200)
                    .body("size()", is(1));
        });
    }

    @Test
    void testRequestReply() {
        RestAssured.get("/request-reply").then()
                .assertThat().statusCode(200)
                .body(is("HELLO"));
    }
}
