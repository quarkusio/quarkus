package org.acme;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class GrpcCompressionInterceptorIntegrationO2OIT extends GrpcCompressionInterceptorIntegrationItTestBase {

    @Override
    protected Channel getChannel() {
        return (channel = GRPCTestUtils.channel(null, getPort()));
    }

    @Override
    protected int getPort() {
        return 9000; // prod
    }

}
