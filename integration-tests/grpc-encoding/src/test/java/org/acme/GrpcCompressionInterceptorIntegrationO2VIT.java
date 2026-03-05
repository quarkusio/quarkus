package org.acme;

import java.util.Map;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(GrpcCompressionInterceptorIntegrationO2VIT.GzipCompressionProfile.class)
class GrpcCompressionInterceptorIntegrationO2VIT extends GrpcCompressionInterceptorIntegrationItTestBase {

    // Java client to Vertx server
    public static class GzipCompressionProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.grpc.server.use-separate-server", "false");
        }
    }

    @Override
    protected Channel getChannel() {
        return (channel = GRPCTestUtils.channel(null, getPort()));
    }

    @Override
    protected int getPort() {
        return 8081;
    }

}
