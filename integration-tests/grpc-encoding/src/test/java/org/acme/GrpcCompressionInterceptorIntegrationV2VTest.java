package org.acme;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GrpcCompressionInterceptorIntegrationV2VTest.GzipCompressionProfile.class)
class GrpcCompressionInterceptorIntegrationV2VTest extends GrpcCompressionInterceptorIntegrationCdiTestBase {

    // Vertx client and server
    public static class GzipCompressionProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.grpc.server.use-separate-server", "false",
                    "quarkus.grpc.clients.hello-service.use-quarkus-grpc-client", "true");
        }
    }
}
