package io.quarkus.grpc.examples.hello;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

abstract class RandomPortVertxServerPlainTestBase extends RandomPortTestBase {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.grpc.server.use-separate-server", "false",
                    "quarkus.http.port", "0",
                    "quarkus.http.test-port", "0",
                    "quarkus.grpc.clients.hello.host", "localhost");
        }
    }
}
