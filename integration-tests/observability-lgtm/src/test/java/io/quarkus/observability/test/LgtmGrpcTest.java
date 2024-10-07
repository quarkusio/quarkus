package io.quarkus.observability.test;

import java.util.Map;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Simple case were we use gRPC as a OTLP protocol
 */
@QuarkusTest
@TestProfile(LgtmGrpcTest.GrpcTestProfileOnly.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmGrpcTest extends LgtmTestBase {

    public static class GrpcTestProfileOnly implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.observability.lgtm.otlp-protocol", "grpc");
        }
    }
}
