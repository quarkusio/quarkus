package io.quarkus.vertx.runtime.tests;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class VertxProducerResourceTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/vertx-test").then()
                .body(containsString("vertx=true"), containsString("eventbus=true"));
    }

    @Test
    public void testEventBus() {
        RestAssured.when().get("/vertx-test/eventBus").then()
                .body(containsString("hello quarkus"));
    }

}
