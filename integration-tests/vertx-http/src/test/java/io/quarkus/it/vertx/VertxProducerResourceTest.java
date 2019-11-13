package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class VertxProducerResourceTest {

    @Test
    public void testInjection() {
        get("/").then().body(containsString("vert.x has been injected"));
    }

    @Test
    public void testInjectedRouter() {
        RestAssured.given().contentType("text/plain").body("Hello world!")
                .post("/").then().body(is("Hello world!"));
    }

    @Test
    public void testRouteRegistration() {
        get("/my-path").then().body(containsString("OK"));
    }

}
