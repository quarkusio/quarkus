package io.quarkus.grpc.examples.hello;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

abstract class RandomPortSeparateServerTlsTestBase extends RandomPortTestBase {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.grpc.server.plain-text", "false",
                    "quarkus.grpc.server.test-port", "0",
                    "quarkus.grpc.server.ssl.certificate", "tls/server.pem",
                    "quarkus.grpc.server.ssl.key", "tls/server.key",
                    "quarkus.grpc.clients.hello.host", "localhost",
                    "quarkus.grpc.clients.hello.ssl.trust-store", "tls/ca.pem");
        }
    }

    @Override
    protected String serverPortProperty() {
        return "quarkus.grpc.server.test-port";
    }
}
