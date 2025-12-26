package io.quarkus.virtual.vertx.web;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Test
    void testRouteOnVirtualThread() {
        String bodyStr = get("/hello").then().statusCode(200).extract().asString();
        // Quarkus specific - all VTs shares the same prefix
        assertTrue(bodyStr.startsWith("quarkus-virtual-thread"));
        assertTrue(get("/hello").then().statusCode(200).extract().asString().startsWith("quarkus-virtual-thread"));
    }

    @Test
    void testRouteOnEventLoop() {
        assertEquals("pong", get("/ping").then().statusCode(200).extract().asString());
    }

    @Test
    void testRouteOnWorker() {
        assertEquals("pong", get("/blocking-ping").then().statusCode(200).extract().asString());
    }

}
