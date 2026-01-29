package io.quarkus.virtual.vertx.web;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

@QuarkusTest
@TestProfile(RunOnVirtualThreadTest.CustomVirtualThreadProfile.class)
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    public static class CustomVirtualThreadProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.virtual-threads.name-prefix", "quarkus-virtual-thread-");
        }
    }

    @Test
    void testRouteOnVirtualThread() {
        String bodyStr = get("/hello").then().statusCode(200).extract().asString();
        String secondBodyStr = get("/hello").then().statusCode(200).extract().asString();
        // Quarkus specific - all VTs shares the same prefix
        assertTrue(bodyStr.startsWith("quarkus-virtual-thread-"));
        assertTrue(secondBodyStr.startsWith("quarkus-virtual-thread-"));
        // Each VT has a unique name in quarkus
        assertNotEquals(bodyStr, secondBodyStr);
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
