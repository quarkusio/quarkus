package io.quarkus.grpc.example.interceptors;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;

@QuarkusTest
class HelloWorldServiceTest extends HelloWorldServiceTestBase {

    @Inject
    Vertx vertx;

    protected Vertx vertx() {
        return vertx;
    }
}
