package io.quarkus.vertx;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;

/**
 * Verify that methods annotated with {@link ConsumeEvent} are called on duplicated contexts and that they handled
 * them properly.
 */
public class DuplicatedContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyConsumers.class));
    private Vertx vertx;

    @BeforeEach
    public void init() {
        vertx = Vertx.vertx();
        vertx.createHttpServer()
                .requestHandler(req -> req.response().end("hey!"))
                .listen(8082).toCompletionStage().toCompletableFuture().join();
    }

    @AfterEach
    public void cleanup() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @Inject
    EventBus bus;

    @Inject
    MyConsumers consumers;

    @Test
    public void testThatMessageSentToTheEventBusAreProcessedOnUnsharedDuplicatedContext() {
        AtomicInteger expected = new AtomicInteger();
        String id1 = UUID.randomUUID().toString();
        bus.send("context-send", id1);
        await().until(() -> consumers.probes().contains(id1));

        consumers.reset();

        String id2 = UUID.randomUUID().toString();
        bus.send("context-send-blocking", id2);
        await().until(() -> consumers.probes().contains(id2));

        consumers.reset();

        String id3 = UUID.randomUUID().toString();
        bus.publish("context-publish", id3);
        await().until(() -> consumers.probes().size() == 2);
        await().until(() -> consumers.probes().contains(id3));

        consumers.reset();

        String id4 = UUID.randomUUID().toString();
        bus.publish("context-publish-blocking", id4);
        await().until(() -> consumers.probes().size() == 2);
        await().until(() -> consumers.probes().contains(id4));
    }

    @Test
    public void testThatEventConsumersAreCalledOnDuplicatedContext() {
        // Creates a bunch of requests that will be executed concurrently.
        // So, we are sure that event loops are reused.
        List<Uni<Void>> unis = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String uuid = UUID.randomUUID().toString();
            unis.add(
                    bus.<String> request("context", uuid)
                            .map(Message::body)
                            .invoke(resp -> {
                                Assertions.assertEquals(resp, "OK-" + uuid);
                            })
                            .replaceWithVoid());
        }

        Uni.join().all(unis).andFailFast()
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .await().atMost(Duration.ofSeconds(10));
    }

    @Test
    public void testThatBlockingEventConsumersAreCalledOnDuplicatedContext() {
        // Creates a bunch of requests that will be executed concurrently.
        // So, we are sure that event loops are reused.
        List<Uni<Void>> unis = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String uuid = UUID.randomUUID().toString();
            unis.add(
                    bus.<String> request("context-blocking", uuid)
                            .map(Message::body)
                            .invoke(resp -> {
                                Assertions.assertEquals(resp, "OK-" + uuid);
                            })
                            .replaceWithVoid());
        }

        Uni.join().all(unis).andFailFast()
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .await().atMost(Duration.ofSeconds(10));

    }

    @ApplicationScoped
    public static class MyConsumers {

        @Inject
        Vertx vertx;

        private List<String> probes = new CopyOnWriteArrayList<>();

        @ConsumeEvent(value = "context")
        Uni<String> receive(String data) {
            Assertions.assertTrue(Thread.currentThread().getName().contains("vert.x-eventloop"));
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");
            return process(data);
        }

        private Uni<String> process(String id) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);

            context.putLocal("key", id);

            return Uni.createFrom().completionStage(
                    () -> vertx.createHttpClient().request(HttpMethod.GET, 8082, "localhost", "/hey")
                            .compose(request -> request.end().compose(x -> request.response()))
                            .compose(HttpClientResponse::body)
                            .map(Buffer::toString)
                            .map(msg -> {
                                Assertions.assertEquals("hey!", msg);
                                Assertions.assertEquals(id, ContextLocals.get("key", null));
                                Assertions.assertSame(Vertx.currentContext(), context);
                                VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");
                                return "OK-" + ContextLocals.get("key", null);
                            }).toCompletionStage());
        }

        @ConsumeEvent(value = "context-blocking")
        @Blocking
        String receiveBlocking(String data) {
            Assertions.assertFalse(Thread.currentThread().getName().contains("vert.x-eventloop"));
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");
            return process(data).await().atMost(Duration.ofSeconds(4));
        }

        @ConsumeEvent(value = "context-send")
        public void consumeSend(String s) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);
            context.putLocal("key", s);

            probes.add(s);
        }

        @ConsumeEvent(value = "context-send-blocking")
        public void consumeSendBlocking(String s) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);
            context.putLocal("key", s);

            probes.add(s);
        }

        @ConsumeEvent(value = "context-publish")
        public void consumePublish1(String s) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);
            context.putLocal("key", s);

            probes.add(s);
        }

        @ConsumeEvent(value = "context-publish")
        public void consumePublish2(String s) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);
            context.putLocal("key", s);

            probes.add(s);
        }

        @ConsumeEvent(value = "context-publish-blocking")
        @Blocking
        public void consumePublishBlocking1(String s) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);
            context.putLocal("key", s);

            probes.add(s);
        }

        @ConsumeEvent(value = "context-publish-blocking")
        @Blocking
        public void consumePublishBlocking2(String s) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("Not marked as safe", "Not marked as safe");

            String val = ContextLocals.get("key", null);
            Assertions.assertNull(val);
            context.putLocal("key", s);

            probes.add(s);
        }

        public List<String> probes() {
            return probes;
        }

        public void reset() {
            probes.clear();
        }

    }

}
