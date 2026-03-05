package org.acme;

import java.util.Map;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@TestProfile(GrpcCompressionInterceptorIntegrationV2VIT.GzipCompressionProfile.class)
class GrpcCompressionInterceptorIntegrationV2VIT extends GrpcCompressionInterceptorIntegrationItTestBase {

    // Vertx client to Vertx server
    public static class GzipCompressionProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.grpc.server.use-separate-server", "false");
        }
    }

    @Override
    protected Channel getChannel() {
        vertx = Vertx.vertx();
        return (channel = GRPCTestUtils.channel(vertx));
    }

    @Override
    protected int getPort() {
        return 8081;
    }

}
