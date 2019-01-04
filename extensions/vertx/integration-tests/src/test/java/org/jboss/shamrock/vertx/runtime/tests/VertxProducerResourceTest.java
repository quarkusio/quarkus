package org.jboss.shamrock.vertx.runtime.tests;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

@RunWith(ShamrockTest.class)
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