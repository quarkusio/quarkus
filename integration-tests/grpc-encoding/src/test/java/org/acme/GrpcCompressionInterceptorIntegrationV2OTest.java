package org.acme;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GrpcCompressionInterceptorIntegrationV2OTest.GzipCompressionProfile.class)
class GrpcCompressionInterceptorIntegrationV2OTest extends GrpcCompressionInterceptorIntegrationCdiTestBase {

    // Vertx client to Java server
    public static class GzipCompressionProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.grpc.clients.hello-service.use-quarkus-grpc-client", "true");
        }
    }
}
