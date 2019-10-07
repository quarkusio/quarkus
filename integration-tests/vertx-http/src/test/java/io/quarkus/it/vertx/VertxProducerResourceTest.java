package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class VertxProducerResourceTest {

    @Test
    public void testInjection() {
        get("/").then().body(containsString("vert.x has been injected"));
    }

    @Test
    public void testRouteRegistration() {
        get("/my-path").then().body(containsString("OK"));
    }

}
