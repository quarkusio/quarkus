package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
class HelloWorldO2NServiceTest extends HelloWorldNewServiceTestBase {

    @Inject
    Vertx vertx;

    @Override
    protected int port() {
        return 8081;
    }

    @Override
    protected void checkVerticles() {
        checkVerticles(vertx);
    }
}
