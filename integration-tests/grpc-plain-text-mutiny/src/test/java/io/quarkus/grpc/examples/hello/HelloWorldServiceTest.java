package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;

@QuarkusTest
class HelloWorldServiceTest extends HelloWorldServiceTestBase {
    @Inject
    Vertx vertx;

    @Override
    protected Vertx vertx() {
        return vertx;
    }
}
