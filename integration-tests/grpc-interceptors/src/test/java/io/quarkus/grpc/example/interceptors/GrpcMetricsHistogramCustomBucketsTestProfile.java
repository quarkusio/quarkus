package io.quarkus.grpc.example.interceptors;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class GrpcMetricsHistogramCustomBucketsTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.micrometer.binder.grpc-server.histogram", "true",
                "quarkus.micrometer.binder.grpc-server.slos", "25ms,50ms",
                "quarkus.micrometer.binder.grpc-server.minimum-expected-value", "10ms",
                "quarkus.micrometer.binder.grpc-server.maximum-expected-value", "100ms",
                "quarkus.micrometer.binder.grpc-client.histogram", "true",
                "quarkus.micrometer.binder.grpc-client.slos", "25ms,50ms",
                "quarkus.micrometer.binder.grpc-client.minimum-expected-value", "10ms",
                "quarkus.micrometer.binder.grpc-client.maximum-expected-value", "100ms");
    }
}
