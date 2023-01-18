package io.quarkus.grpc.example.interceptors;

import jakarta.inject.Inject;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class HelloWorldServiceVertxTest extends HelloWorldServiceTestBase {

    @Inject
    Vertx vertx;

    protected Vertx vertx() {
        return vertx;
    }
}
