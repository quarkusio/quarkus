package io.quarkus.grpc.example.interceptors;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class GrpcMetricsHistogramTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.micrometer.binder.grpc-server.histogram", "true",
                "quarkus.micrometer.binder.grpc-client.histogram", "true");
    }
}
