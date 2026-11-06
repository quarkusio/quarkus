package io.quarkus.it.vertx;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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
    public void testDnsResolver() {
        RestAssured.when().get("/vertx-test/dns").then()
                .statusCode(200).body(equalTo("true"));
    }

    @Test
    public void testEventBus() {
        RestAssured.when().get("/vertx-test/eventBus").then()
                .body(containsString("hello quarkus"));
    }

}
