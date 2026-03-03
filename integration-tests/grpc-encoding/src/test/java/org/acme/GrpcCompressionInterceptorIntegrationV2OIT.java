package org.acme;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
class GrpcCompressionInterceptorIntegrationV2OIT extends GrpcCompressionInterceptorIntegrationItTestBase {

    @Override
    protected Channel getChannel() {
        vertx = Vertx.vertx();
        return (channel = GRPCTestUtils.channel(vertx, getPort()));
    }

    @Override
    protected int getPort() {
        return 9000; // prod
    }

}
