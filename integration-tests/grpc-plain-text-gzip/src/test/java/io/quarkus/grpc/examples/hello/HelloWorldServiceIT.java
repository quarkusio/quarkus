package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
class HelloWorldServiceIT extends HelloWorldServiceTestBase {

    private Vertx vertx;

    @Override
    protected Vertx vertx() {
        vertx = Vertx.vertx();
        return vertx;
    }

    @Override
    protected void close(Vertx vertx) {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
