package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@TestProfile(N2OGRPCTestProfile.class)
class HelloWorldN2OServiceIT extends HelloWorldNewServiceTestBase {
    @Override
    protected int port() {
        return 9001;
    }

    @Override
    protected Vertx vertx() {
        return Vertx.vertx();
    }

    @Override
    protected void close(Vertx vertx) {
        GRPCTestUtils.close(vertx);
    }
}
