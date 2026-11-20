package io.quarkus.smallrye.reactivemessaging.blocking;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.context.ArcContextProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.reactive.messaging.PublisherDecorator;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.connector.InboundConnector;
import io.smallrye.reactive.messaging.providers.locals.ContextAwareMessage;
import io.smallrye.reactive.messaging.providers.locals.LocalContextMetadata;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class BlockingActivateRequestContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    RequestScopedBean.class,
                    DuplicatedContextConnector.class,
                    EventLoopBracketPublisherDecorator.class,
                    BlockingConsumer.class,
                    DirectActivateRequestContextBean.class,
                    AwaitEventLoopBracket.class,
                    AwaitEventLoopBracketInterceptor.class))
            .overrideConfigKey("mp.context.ThreadContext.cleared", "Transaction,CDI")
            .overrideConfigKey("mp.messaging.incoming.in.connector", "duplicated-context-connector");

    @Inject
    Vertx vertx;

    @Inject
    @Connector("duplicated-context-connector")
    DuplicatedContextConnector connector;

    @Inject
    BlockingConsumer consumer;

    @Inject
    DirectActivateRequestContextBean directBean;

    @BeforeEach
    void setUp() {
        Arc.container().requestContext().terminate();
        RequestScopedBean.resetCounters();
        consumer.reset();
    }

    @Test
    void testBlockingIncomingWithActivateRequestContextOnDuplicatedContext() throws Exception {
        CompletableFuture<Context> dcFuture = new CompletableFuture<>();
        Context baseContext = vertx.getOrCreateContext();
        baseContext.runOnContext(v -> {
            Context dc = VertxContext.getOrCreateDuplicatedContext(baseContext);
            VertxContextSafetyToggle.setContextSafe(dc, true);
            dcFuture.complete(dc);
        });
        Context duplicatedContext = dcFuture.get(5, TimeUnit.SECONDS);

        connector.sendOnContext(duplicatedContext, "blocking-payload");

        assertThat(consumer.awaitCompletion()).isTrue();
        assertThat(consumer.getFailure()).isNull();
        assertThat(consumer.getValueBeforeBracketEnd()).isEqualTo("blocking-payload");
        assertThat(consumer.getValueAfterBracketEnd()).isEqualTo("blocking-payload");
        assertThat(consumer.getIdBeforeBracketEnd()).isNotNull().isEqualTo(consumer.getIdAfterBracketEnd());
        assertThat(consumer.getDestroyedCountDuringExecution()).isZero();
        assertThat(RequestScopedBean.DESTROYED_COUNT.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testDirectClearContextSnapshotAndActivateRequestContextOverlap(boolean accessBeforeEndContext) throws Exception {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        Context baseContext = vertx.getOrCreateContext();
        baseContext.runOnContext(v -> {
            Context dc = VertxContext.getOrCreateDuplicatedContext(baseContext);
            VertxContextSafetyToggle.setContextSafe(dc, true);
            dc.runOnContext(ignored -> {
                ThreadContextSnapshot clearedSnapshot = new ArcContextProvider().clearedContext(Map.of());
                ThreadContextController controller = clearedSnapshot.begin();

                dc.<String> executeBlocking(() -> directBean.executeWithActivateRequestContext(
                        "direct-payload",
                        accessBeforeEndContext,
                        dc,
                        controller))
                        .onComplete(ar -> {
                            if (ar.failed()) {
                                completion.completeExceptionally(ar.cause());
                                return;
                            }
                            try {
                                assertThat(ar.result()).isEqualTo("direct-payload");
                                assertThat(RequestScopedBean.DESTROYED_COUNT.get()).isEqualTo(1);
                                assertThat(Arc.container().requestContext().isActive()).isFalse();
                                completion.complete(null);
                            } catch (Throwable t) {
                                completion.completeExceptionally(t);
                            }
                        });
            });
        });

        completion.get(5, TimeUnit.SECONDS);
    }

    @RequestScoped
    public static class RequestScopedBean {

        static final AtomicInteger CREATED_SEQUENCE = new AtomicInteger();
        static final AtomicInteger DESTROYED_COUNT = new AtomicInteger();

        private String id;
        private String value;

        static void resetCounters() {
            CREATED_SEQUENCE.set(0);
            DESTROYED_COUNT.set(0);
        }

        @PostConstruct
        void init() {
            this.id = "req-" + CREATED_SEQUENCE.incrementAndGet();
        }

        @PreDestroy
        void destroy() {
            DESTROYED_COUNT.incrementAndGet();
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @InterceptorBinding
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AwaitEventLoopBracket {
    }

    @AwaitEventLoopBracket
    @Interceptor
    @Priority(Interceptor.Priority.PLATFORM_BEFORE + 50)
    public static class AwaitEventLoopBracketInterceptor {

        @Inject
        BlockingConsumer consumer;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            try {
                consumer.onWorkerReadyBeforeActivateRequestContext();
                return ctx.proceed();
            } catch (Throwable t) {
                consumer.recordFailure(t);
                if (t instanceof Exception e) {
                    throw e;
                }
                throw (Error) t;
            } finally {
                consumer.markCompleted();
            }
        }
    }

    @ApplicationScoped
    @Connector("duplicated-context-connector")
    public static class DuplicatedContextConnector implements InboundConnector {

        private final BroadcastProcessor<Message<?>> processor = BroadcastProcessor.create();

        @Override
        public Flow.Publisher<? extends Message<?>> getPublisher(Config config) {
            return processor;
        }

        void sendOnContext(Context duplicatedContext, String payload) {
            duplicatedContext.runOnContext(v -> {
                Message<String> message = ContextAwareMessage.of(payload)
                        .withMetadata(Metadata.of(new LocalContextMetadata(duplicatedContext)));
                processor.onNext(message);
            });
        }
    }

    @ApplicationScoped
    public static class EventLoopBracketPublisherDecorator implements PublisherDecorator {

        @Inject
        ThreadContext threadContext;

        @Inject
        BlockingConsumer consumer;

        @Override
        public int getPriority() {
            // Runs after SmallRye's ContextDecorator (priority 0) so onItem executes on the message duplicated context
            return 10;
        }

        @Override
        public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> publisher, List<String> channelName,
                boolean isConnector) {
            if (!isConnector) {
                return publisher;
            }
            Runnable clearedBracketAction = threadContext.contextualRunnable(() -> {
                consumer.markEventLoopBracketBegun();
                assertThat(consumer.awaitFirstBeanAccess()).isTrue();
            });
            return new BracketOperator<>(publisher, clearedBracketAction, consumer);
        }

        static final class BracketOperator<T extends Message<?>> extends AbstractMultiOperator<T, T> {

            private final Runnable clearedBracketAction;
            private final BlockingConsumer consumer;

            BracketOperator(Multi<? extends T> upstream, Runnable clearedBracketAction, BlockingConsumer consumer) {
                super(upstream);
                this.clearedBracketAction = clearedBracketAction;
                this.consumer = consumer;
            }

            @Override
            public void subscribe(MultiSubscriber<? super T> downstream) {
                upstream.subscribe().withSubscriber(new MultiOperatorProcessor<T, T>(downstream) {
                    @Override
                    public void onItem(T item) {
                        super.onItem(item);
                        assertThat(consumer.awaitWorkerReady()).isTrue();
                        try {
                            clearedBracketAction.run();
                        } finally {
                            consumer.markEventLoopBracketEnded();
                        }
                    }
                });
            }
        }
    }

    @ApplicationScoped
    public static class BlockingConsumer {

        @Inject
        RequestScopedBean requestBean;

        private volatile CountDownLatch workerReady = new CountDownLatch(1);
        private volatile CountDownLatch eventLoopBracketBegun = new CountDownLatch(1);
        private volatile CountDownLatch firstBeanAccessDone = new CountDownLatch(1);
        private volatile CountDownLatch eventLoopBracketEnded = new CountDownLatch(1);
        private volatile CountDownLatch completed = new CountDownLatch(1);

        private final AtomicReference<String> idBeforeBracketEnd = new AtomicReference<>();
        private final AtomicReference<String> idAfterBracketEnd = new AtomicReference<>();
        private final AtomicReference<String> valueBeforeBracketEnd = new AtomicReference<>();
        private final AtomicReference<String> valueAfterBracketEnd = new AtomicReference<>();
        private final AtomicInteger destroyedCountDuringExecution = new AtomicInteger(-1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        void reset() {
            workerReady = new CountDownLatch(1);
            eventLoopBracketBegun = new CountDownLatch(1);
            firstBeanAccessDone = new CountDownLatch(1);
            eventLoopBracketEnded = new CountDownLatch(1);
            completed = new CountDownLatch(1);
            idBeforeBracketEnd.set(null);
            idAfterBracketEnd.set(null);
            valueBeforeBracketEnd.set(null);
            valueAfterBracketEnd.set(null);
            destroyedCountDuringExecution.set(-1);
            failure.set(null);
        }

        void onWorkerReadyBeforeActivateRequestContext() throws InterruptedException {
            workerReady.countDown();
            if (!eventLoopBracketBegun.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for event-loop ClearContextSnapshot bracket to begin");
            }
        }

        @Incoming("in")
        @Blocking
        @ActivateRequestContext
        @AwaitEventLoopBracket
        public void consume(String payload) {
            try {
                requestBean.setValue(payload);
                idBeforeBracketEnd.set(requestBean.getId());
                valueBeforeBracketEnd.set(requestBean.getValue());
                firstBeanAccessDone.countDown();

                if (!eventLoopBracketEnded.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for event-loop ClearContextSnapshot bracket to end");
                }

                idAfterBracketEnd.set(requestBean.getId());
                valueAfterBracketEnd.set(requestBean.getValue());
                destroyedCountDuringExecution.set(RequestScopedBean.DESTROYED_COUNT.get());
            } catch (Throwable t) {
                recordFailure(t);
            }
        }

        void recordFailure(Throwable t) {
            failure.compareAndSet(null, t);
        }

        boolean awaitWorkerReady() {
            try {
                return workerReady.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        void markEventLoopBracketBegun() {
            eventLoopBracketBegun.countDown();
        }

        boolean awaitFirstBeanAccess() {
            try {
                return firstBeanAccessDone.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        void markEventLoopBracketEnded() {
            eventLoopBracketEnded.countDown();
        }

        void markCompleted() {
            completed.countDown();
        }

        boolean awaitCompletion() throws InterruptedException {
            return completed.await(5, TimeUnit.SECONDS);
        }

        String getIdBeforeBracketEnd() {
            return idBeforeBracketEnd.get();
        }

        String getIdAfterBracketEnd() {
            return idAfterBracketEnd.get();
        }

        String getValueBeforeBracketEnd() {
            return valueBeforeBracketEnd.get();
        }

        String getValueAfterBracketEnd() {
            return valueAfterBracketEnd.get();
        }

        int getDestroyedCountDuringExecution() {
            return destroyedCountDuringExecution.get();
        }

        Throwable getFailure() {
            return failure.get();
        }
    }

    @ApplicationScoped
    public static class DirectActivateRequestContextBean {

        @Inject
        RequestScopedBean requestBean;

        @ActivateRequestContext
        public String executeWithActivateRequestContext(
                String payload,
                boolean accessBeforeEndContext,
                Context duplicatedContext,
                ThreadContextController controller) throws Exception {
            String initialId = null;
            if (accessBeforeEndContext) {
                requestBean.setValue(payload);
                initialId = requestBean.getId();
                assertThat(RequestScopedBean.DESTROYED_COUNT.get()).isZero();
            }

            CountDownLatch endedLatch = new CountDownLatch(1);
            duplicatedContext.runOnContext(v -> {
                try {
                    controller.endContext();
                } finally {
                    endedLatch.countDown();
                }
            });
            assertThat(endedLatch.await(5, TimeUnit.SECONDS)).isTrue();

            assertThat(Arc.container().requestContext().isActive()).isTrue();
            if (!accessBeforeEndContext) {
                requestBean.setValue(payload);
                initialId = requestBean.getId();
            }

            assertThat(RequestScopedBean.DESTROYED_COUNT.get()).isZero();
            assertThat(requestBean.getId()).isEqualTo(initialId);
            return requestBean.getValue();
        }
    }
}
