package io.quarkus.grpc.examples.hello;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;

@QuarkusTest
class HelloWorldMutualTlsEndpointTest extends VertxHelloWorldMutualTlsEndpointTestBase {

    @Inject
    Vertx vertx;

    @Override
    Vertx vertx() {
        return vertx;
    }
}
