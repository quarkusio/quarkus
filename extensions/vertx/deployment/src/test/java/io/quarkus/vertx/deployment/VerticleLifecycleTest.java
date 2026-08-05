package io.quarkus.vertx.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.mutiny.core.Vertx;

public class VerticleLifecycleTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(LifecycleVerticle.class));

    @Inject
    Vertx vertx;

    @Test
    public void testVerticleDeployAndMessageExchange() {
        String deploymentId = vertx.deployVerticle(new LifecycleVerticle())
                .await().indefinitely();

        assertThat(deploymentId).isNotNull().isNotEmpty();

        String reply = vertx.eventBus().<String> request("lifecycle-test", "ping")
                .onItem().transform(m -> m.body())
                .await().indefinitely();

        assertThat(reply).isEqualTo("pong");
    }

    @Test
    public void testVerticleUndeploy() {
        LifecycleVerticle verticle = new LifecycleVerticle();

        String deploymentId = vertx.deployVerticle(verticle)
                .await().indefinitely();

        assertThat(deploymentId).isNotNull();

        String reply = vertx.eventBus().<String> request("lifecycle-test", "ping")
                .onItem().transform(m -> m.body())
                .await().indefinitely();
        assertThat(reply).isEqualTo("pong");

        vertx.undeploy(deploymentId).await().indefinitely();
        assertThat(verticle.stopped.get()).isTrue();
    }

    @Test
    public void testVerticleStartFailure() {
        assertThatThrownBy(() -> vertx.deployVerticle(new FailingVerticle())
                .await().indefinitely())
                .hasMessageContaining("Intentional start failure");
    }

    @Test
    public void testMultipleVerticleInstances() {
        io.vertx.core.Vertx coreVertx = vertx.getDelegate();
        CompletableFuture<String> future = coreVertx.deployVerticle(LifecycleVerticle.class.getName(),
                new io.vertx.core.DeploymentOptions().setInstances(3))
                .toCompletionStage().toCompletableFuture();

        String deploymentId = future.join();
        assertThat(deploymentId).isNotNull();

        // All instances should respond (the event bus round-robins to one of them)
        for (int i = 0; i < 5; i++) {
            String reply = vertx.eventBus().<String> request("lifecycle-test", "ping")
                    .onItem().transform(m -> m.body())
                    .await().indefinitely();
            assertThat(reply).isEqualTo("pong");
        }

        vertx.undeploy(deploymentId).await().indefinitely();
    }

    public static class LifecycleVerticle extends AbstractVerticle {

        final AtomicBoolean stopped = new AtomicBoolean(false);

        @Override
        public void start(Promise<Void> startPromise) {
            vertx.eventBus().localConsumer("lifecycle-test", msg -> {
                if ("ping".equals(msg.body())) {
                    msg.reply("pong");
                }
            });
            startPromise.complete();
        }

        @Override
        public void stop(Promise<Void> stopPromise) {
            stopped.set(true);
            stopPromise.complete();
        }
    }

    public static class FailingVerticle extends AbstractVerticle {
        @Override
        public void start(Promise<Void> startPromise) {
            startPromise.fail(new RuntimeException("Intentional start failure"));
        }
    }

}
