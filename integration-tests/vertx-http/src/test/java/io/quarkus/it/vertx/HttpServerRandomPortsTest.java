package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.it.vertx.HttpServerRandomPortsTest.Profile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
@TestProfile(Profile.class)
class HttpServerRandomPortsTest {
    @Test
    void httpServer(HttpServer httpServer) {
        assertTrue(httpServer.getPort() > 0);
        assertTrue(httpServer.getPort() != 8080);
        assertTrue(httpServer.getPort() != 8081);
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.port", "0",
                    "quarkus.http.test-port", "0");
        }
    }
}
