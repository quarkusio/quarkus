package io.quarkus.arc.test.contexts.currentmanaged;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.impl.ContextInstances;
import io.quarkus.arc.impl.CurrentManagedContext;
import io.quarkus.arc.impl.CurrentManagedContext.CurrentContextState;

/**
 * Reproduces the once-only violation in {@code CurrentContextState.set(byte)}: under concurrent
 * {@code destroy(ContextState)}, the {@code @Destroyed} / beforeDestroyed notifiers must each fire
 * exactly once. Fails on the pre-fix {@code compareAndExchange} witness bug; passes with the fix.
 */
public class CurrentManagedContextConcurrentDestroyTest {

    private static final int THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());

    private static final int ROUNDS = 1000;

    @Test
    public void destroyedFiresExactlyOnceUnderConcurrentDestroy() throws Exception {
        AtomicInteger destroyedCount = new AtomicInteger();
        AtomicInteger beforeDestroyedCount = new AtomicInteger();

        // Trivial ContextInstances (no beans); destroy(state) only iterates removeEach, a no-op here.
        Supplier<ContextInstances> instances = () -> new ContextInstances() {
            @Override
            public ContextInstanceHandle<?> computeIfAbsent(String id, Supplier<ContextInstanceHandle<?>> supplier) {
                return supplier.get();
            }

            @Override
            public ContextInstanceHandle<?> getIfPresent(String id) {
                return null;
            }

            @Override
            public ContextInstanceHandle<?> remove(String id) {
                return null;
            }

            @Override
            public Set<ContextInstanceHandle<?>> getAllPresent() {
                return Collections.emptySet();
            }

            @Override
            public void removeEach(Consumer<? super ContextInstanceHandle<?>> action) {
                // no contextual instances to destroy
            }
        };

        // destroy(ContextState) never reads the CurrentContext, so a no-op impl is sufficient.
        CurrentContext<CurrentContextState> currentContext = new CurrentContext<CurrentContextState>() {
            private volatile CurrentContextState state;

            @Override
            public CurrentContextState get() {
                return state;
            }

            @Override
            public void set(CurrentContextState state) {
                this.state = state;
            }

            @Override
            public void remove() {
                this.state = null;
            }
        };

        CurrentManagedContext ctx = new CurrentManagedContext(currentContext, instances, null,
                o -> beforeDestroyedCount.incrementAndGet(), o -> destroyedCount.incrementAndGet()) {
            @Override
            public Class<? extends Annotation> getScope() {
                return RequestScoped.class;
            }

            @Override
            protected ContextNotActiveException notActive() {
                return new ContextNotActiveException("test context not active");
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                destroyedCount.set(0);
                beforeDestroyedCount.set(0);

                // Fresh, valid state each round; initializeState() does not touch the CurrentContext.
                CurrentContextState state = ctx.initializeState();

                CountDownLatch start = new CountDownLatch(1);
                Future<?>[] futures = new Future<?>[THREADS];
                for (int t = 0; t < THREADS; t++) {
                    futures[t] = pool.submit(() -> {
                        start.await();
                        ctx.destroy(state);
                        return null;
                    });
                }

                start.countDown();
                for (Future<?> f : futures) {
                    f.get(10, TimeUnit.SECONDS);
                }

                assertEquals(1, destroyedCount.get(),
                        "@Destroyed notifier fired " + destroyedCount.get() + " times in round " + round);
                assertEquals(1, beforeDestroyedCount.get(),
                        "beforeDestroyed notifier fired " + beforeDestroyedCount.get() + " times in round " + round);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
