package io.quarkus.virtual.vertx.web;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Test
    void testRoute() {
        String bodyStr = get("/hello").then().statusCode(200).extract().asString();
        // Each VT has a unique name in quarkus
        assertNotEquals(bodyStr, get("/hello").then().statusCode(200).extract().asString());

    }

}
