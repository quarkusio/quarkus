package org.acme;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GrpcCompressionInterceptorIntegrationO2VTest.GzipCompressionProfile.class)
class GrpcCompressionInterceptorIntegrationO2VTest extends GrpcCompressionInterceptorIntegrationCdiTestBase {

    // Java client to Vertx server
    public static class GzipCompressionProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.grpc.server.use-separate-server", "false",
                    "quarkus.grpc.clients.hello-service.test-port", "8081");
        }
    }
}
