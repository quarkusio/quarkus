package io.quarkus.grpc.examples.hello;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

abstract class RandomPortSeparateServerPlainTestBase extends RandomPortTestBase {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.grpc.server.test-port", "0",
                    "quarkus.grpc.clients.hello.host", "localhost");
        }
    }

    @Override
    protected String serverPortProperty() {
        return "quarkus.grpc.server.test-port";
    }
}
