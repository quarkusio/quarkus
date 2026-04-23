package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

/**
 * Sends many signals concurrently from multiple threads across all emission modes
 * (publish, send, request) and all execution models (blocking, non-blocking, virtual thread),
 * and verifies that every CDI request context activated by the built-in
 * {@code RequestContextInterceptor} is terminated correctly.
 */
public class ConcurrentRequestContextTest {

    static final int CONCURRENCY = 1000;

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Cmd.class,
                    BlockingReceiver.class, ReactiveReceiver.class, VtReceiver.class,
                    RequestScopedTracker.class,
                    Blocking.class, Blocking.Literal.class,
                    Reactive.class, Reactive.Literal.class,
                    Vt.class, Vt.Literal.class));

    @Inject
    Signal<Cmd> signal;

    @Inject
    @Blocking
    Signal<Cmd> blockingSignal;

    @Inject
    @Reactive
    Signal<Cmd> reactiveSignal;

    @Inject
    @Vt
    Signal<Cmd> vtSignal;

    @BeforeEach
    void reset() {
        RequestScopedTracker.ID_GENERATOR.set(0);
        RequestScopedTracker.CREATED.clear();
        RequestScopedTracker.DESTROYED.clear();
    }

    @Test
    void testConcurrentEmissions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < CONCURRENCY; i++) {
                final int id = i;

                // publish - delivers to all three receivers
                futures.add(executor.submit(() -> signal.publishUni(new Cmd(id))
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely()));

                // send - delivers to one receiver (round-robin)
                futures.add(executor.submit(() -> signal.sendUni(new Cmd(id))
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely()));

                // request - use qualifiers to target each execution model
                futures.add(executor.submit(() -> {
                    String result = blockingSignal.requestUni(new Cmd(id), String.class)
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely();
                    assertThat(result).isEqualTo("blocking-" + id);
                }));
                futures.add(executor.submit(() -> {
                    String result = reactiveSignal.requestUni(new Cmd(id), String.class)
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely();
                    assertThat(result).isEqualTo("reactive-" + id);
                }));
                futures.add(executor.submit(() -> {
                    String result = vtSignal.requestUni(new Cmd(id), String.class)
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely();
                    assertThat(result).isEqualTo("vt-" + id);
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        // publish hits 3 receivers, send hits 1, and 3 qualified requests hit 1 each
        int expectedContexts = CONCURRENCY * (3 + 1 + 3);
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(RequestScopedTracker.CREATED)
                            .as("Each receiver invocation must get a unique request-scoped bean")
                            .hasSize(expectedContexts);
                    assertThat(RequestScopedTracker.DESTROYED)
                            .as("All request-scoped beans must be destroyed")
                            .isEqualTo(RequestScopedTracker.CREATED);
                });
    }

    @Singleton
    public static class BlockingReceiver {

        void onCmd(@Receives Cmd cmd, RequestScopedTracker tracker) {
            tracker.touch();
        }

        String onBlockingCmd(@Receives @Blocking Cmd cmd, RequestScopedTracker tracker) {
            tracker.touch();
            return "blocking-" + cmd.id();
        }
    }

    @Singleton
    public static class ReactiveReceiver {

        Uni<Void> onCmd(@Receives Cmd cmd, RequestScopedTracker tracker) {
            tracker.touch();
            return Uni.createFrom().voidItem();
        }

        Uni<String> onReactiveCmd(@Receives @Reactive Cmd cmd, RequestScopedTracker tracker) {
            tracker.touch();
            return Uni.createFrom().item("reactive-" + cmd.id());
        }
    }

    @Singleton
    public static class VtReceiver {

        @RunOnVirtualThread
        void onCmd(@Receives Cmd cmd, RequestScopedTracker tracker) {
            tracker.touch();
        }

        @RunOnVirtualThread
        String onVtCmd(@Receives @Vt Cmd cmd, RequestScopedTracker tracker) {
            tracker.touch();
            return "vt-" + cmd.id();
        }
    }

    @RequestScoped
    public static class RequestScopedTracker {

        static final AtomicInteger ID_GENERATOR = new AtomicInteger();
        static final Set<Integer> CREATED = ConcurrentHashMap.newKeySet();
        static final Set<Integer> DESTROYED = ConcurrentHashMap.newKeySet();

        private int id;

        @PostConstruct
        void init() {
            id = ID_GENERATOR.incrementAndGet();
            CREATED.add(id);
        }

        void touch() {
        }

        @PreDestroy
        void destroy() {
            DESTROYED.add(id);
        }
    }

    record Cmd(int id) {
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @interface Blocking {

        final class Literal extends AnnotationLiteral<Blocking> implements Blocking {
        }
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @interface Reactive {

        final class Literal extends AnnotationLiteral<Reactive> implements Reactive {
        }
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @interface Vt {

        final class Literal extends AnnotationLiteral<Vt> implements Vt {
        }
    }
}
