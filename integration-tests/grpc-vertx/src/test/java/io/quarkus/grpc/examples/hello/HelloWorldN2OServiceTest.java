package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusTest
@TestProfile(N2OGRPCTestProfile.class)
class HelloWorldN2OServiceTest extends HelloWorldNewServiceTestBase {

    @Inject
    Vertx vertx;

    @Override
    protected Vertx vertx() {
        return vertx;
    }

    @Override
    protected int port() {
        return 9001;
    }
}
