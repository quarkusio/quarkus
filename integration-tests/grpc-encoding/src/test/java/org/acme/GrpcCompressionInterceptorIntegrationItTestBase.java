package org.acme;

import org.junit.jupiter.api.AfterEach;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.vertx.core.Vertx;

abstract class GrpcCompressionInterceptorIntegrationItTestBase extends GrpcCompressionInterceptorIntegrationTestBase {

    protected Vertx vertx;
    protected Channel channel;

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(channel);
        channel = null;

        if (vertx != null) {
            GRPCTestUtils.close(vertx);
            vertx = null;
        }
    }

}
