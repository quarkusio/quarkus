package org.acme;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
class GrpcCompressionInterceptorIntegrationV2VIT extends GrpcCompressionInterceptorIntegrationItTestBase {

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
