package io.quarkus.it.vertx;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class NettyMainEventLoopGroupResourceTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/eventloop").then()
                .body(containsString("passed"));
    }
}
