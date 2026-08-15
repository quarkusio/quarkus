package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;

@QuarkusTest
class HelloWorldNewServiceTest extends HelloWorldNewServiceTestBase {

    @Inject
    Vertx vertx;

    @Override
    protected Vertx vertx() {
        return vertx;
    }

    @Override
    protected int port() {
        return 8081;
    }

    @Override
    protected void checkVerticles() {
        checkVerticles(vertx);
    }
}
