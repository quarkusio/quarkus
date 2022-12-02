package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
class HelloWorldVertxServiceIT extends HelloWorldNewServiceTestBase {
    @Override
    protected Vertx vertx() {
        return Vertx.vertx();
    }

    @Override
    protected void close(Vertx vertx) {
        GRPCTestUtils.close(vertx);
    }
}
