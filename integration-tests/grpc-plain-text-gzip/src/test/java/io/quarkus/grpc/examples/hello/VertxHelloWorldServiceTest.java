package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldServiceTest extends HelloWorldServiceTestBase {
    @Inject
    Vertx vertx;

    @Override
    protected Vertx vertx() {
        return vertx;
    }
}
