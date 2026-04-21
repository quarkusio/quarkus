package io.quarkus.grpc.examples.hello;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

abstract class RandomPortVertxServerTlsTestBase extends RandomPortTestBase {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.test-ssl-port", "0",
                    "quarkus.http.ssl.certificate.files", "tls/server.pem",
                    "quarkus.http.ssl.certificate.key-files", "tls/server.key",
                    "quarkus.grpc.clients.hello.host", "localhost",
                    "quarkus.grpc.clients.hello.ssl.trust-store", "tls/ca.pem");
        }
    }
}
