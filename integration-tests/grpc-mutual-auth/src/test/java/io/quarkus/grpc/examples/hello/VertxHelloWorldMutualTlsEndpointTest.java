package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled
class VertxHelloWorldMutualTlsEndpointTest extends VertxHelloWorldMutualTlsEndpointTestBase {

    @Inject
    Vertx vertx;

    @Override
    Vertx vertx() {
        return vertx;
    }
}
