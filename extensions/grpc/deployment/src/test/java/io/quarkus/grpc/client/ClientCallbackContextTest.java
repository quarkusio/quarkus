package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

/**
 * Tests that gRPC async client response callbacks are dispatched onto the correct context
 * depending on where the call was initiated from:
 * <ul>
 * <li>Event-loop root context: callback must land on a <em>duplicated</em> context, not root.
 * Running on root breaks {@code ContextStorageOverride}, which stores the gRPC
 * {@code Context} in context-local storage (only available on duplicated contexts) and
 * falls back to a ThreadLocal otherwise (a ThreadLocal that can be overwritten by
 * interleaving async tasks on the same event-loop thread).</li>
 * <li>Duplicated context: callback must stay on the same duplicated context.</li>
 * <li>Blocking worker thread: {@code IOThreadClientInterceptor} skips rerouting
 * ({@code isOnEventLoopThread()} is false); the call must still complete successfully.</li>
 * <li>Virtual thread: no Vert.x context ({@code Vertx.currentContext() == null}); the
 * interceptor also skips rerouting and the call must complete successfully.</li>
 * </ul>
 */
public class ClientCallbackContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, MyConsumer.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer consumer;

    @Test
    public void testCallbackIsOnDuplicatedContextWhenCalledFromRootContext() {
        boolean isDuplicated = consumer.callFromRootAndCheckCallbackContext();
        assertThat(isDuplicated)
                .as("callback should run on a duplicated Vert.x context, not on the root context")
                .isTrue();
    }

    @Test
    public void testCallbackIsOnSameDuplicatedContextWhenCalledFromDuplicatedContext() {
        boolean isDuplicated = consumer.callFromDuplicatedAndCheckCallbackContext();
        assertThat(isDuplicated)
                .as("callback should remain on the duplicated Vert.x context")
                .isTrue();
    }

    @Test
    public void testCallCompletesFromWorkerThread() {
        String reply = consumer.callFromWorker();
        assertThat(reply).isEqualTo("Hello worker");
    }

    @Test
    public void testCallCompletesFromVirtualThread() throws Exception {
        String reply = consumer.callFromVirtualThread();
        assertThat(reply).isEqualTo("Hello virtual");
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient("hello-service")
        MutinyGreeterGrpc.MutinyGreeterStub stub;

        @Inject
        Vertx vertx;

        public boolean callFromRootAndCheckCallbackContext() {
            Context rootCtx = vertx.getOrCreateContext();
            return stub.sayHello(HelloRequest.newBuilder().setName("root").build())
                    .map(HelloReply::getMessage)
                    .map(ignored -> VertxContext.isDuplicatedContext(Vertx.currentContext().getDelegate()))
                    .runSubscriptionOn(rootCtx::runOnContext).await().atMost(Duration.ofSeconds(5));
        }

        public boolean callFromDuplicatedAndCheckCallbackContext() {
            Context root = vertx.getOrCreateContext();
            io.vertx.core.Context duplicate = VertxContext.getOrCreateDuplicatedContext(root.getDelegate());
            return stub.sayHello(HelloRequest.newBuilder().setName("dup").build())
                    .map(HelloReply::getMessage)
                    .map(ignored -> VertxContext.isDuplicatedContext(Vertx.currentContext().getDelegate()))
                    .runSubscriptionOn(MutinyHelper.executor(duplicate))
                    .await().atMost(Duration.ofSeconds(5));
        }

        public String callFromWorker() {
            return stub.sayHello(HelloRequest.newBuilder().setName("worker").build())
                    .map(HelloReply::getMessage)
                    .runSubscriptionOn(MutinyHelper.blockingExecutor(vertx.getDelegate()))
                    .await().atMost(Duration.ofSeconds(5));
        }

        public String callFromVirtualThread() throws Exception {
            return stub.sayHello(HelloRequest.newBuilder().setName("virtual").build())
                    .map(HelloReply::getMessage)
                    .runSubscriptionOn(Executors.newVirtualThreadPerTaskExecutor())
                    .await().atMost(Duration.ofSeconds(5));
        }
    }
}
