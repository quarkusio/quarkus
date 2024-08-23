package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldServiceIT extends HelloWorldServiceTestBase {
    @Override
    protected Vertx vertx() {
        return Vertx.vertx();
    }

    @Override
    protected void close(Vertx vertx) {
        GRPCTestUtils.close(vertx);
    }
}
