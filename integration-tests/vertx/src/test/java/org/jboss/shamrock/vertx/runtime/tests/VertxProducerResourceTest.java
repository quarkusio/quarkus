package org.jboss.shamrock.vertx.runtime.tests;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

@ShamrockTest
public class VertxProducerResourceTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/vertx-test").then()
                .body(containsString("vertx=true"), containsString("eventbus=true"));
    }

    @Test
    public void testEventBus() {
        RestAssured.when().get("/vertx-test/eventBus").then()
                .body(containsString("hello shamrock"));
    }

}