package io.quarkus.grpc.examples.hello;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled("Enable once load balancing / Stork is ready")
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
