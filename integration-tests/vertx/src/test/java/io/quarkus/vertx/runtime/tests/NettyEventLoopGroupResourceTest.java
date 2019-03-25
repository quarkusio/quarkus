package io.quarkus.vertx.runtime.tests;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class NettyEventLoopGroupResourceTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/vertx-test/eventloop").then()
                .body(containsString("passed"));
    }
}
