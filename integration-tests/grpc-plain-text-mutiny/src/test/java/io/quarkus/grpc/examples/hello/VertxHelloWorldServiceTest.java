package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled("Enable once load balancing / Stork is ready")
class VertxHelloWorldServiceTest extends HelloWorldServiceTestBase {
    @Inject
    Vertx vertx;

    @Override
    protected Vertx vertx() {
        return vertx;
    }
}
