package io.quarkus.vertx.mdc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.jboss.logging.MDC;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

/**
 * A duplicated context created for a nested operation and linked to the context the operation
 * originates from (the way the REST Client creates a per-invocation context: a fresh duplicate
 * of the root context carrying {@link VertxContext#PARENT_CONTEXT_LOCAL}) must inherit the MDC
 * of the originating context, so log statements emitted while the nested operation runs keep
 * the contextual data of the originating request.
 * See <a href="https://github.com/quarkusio/quarkus/issues/55828">GitHub issue #55828</a>.
 */
public class NestedContextMdcTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Vertx vertx;

    @Test
    void parentLinkedContextInheritsMdc() throws InterruptedException {
        AtomicReference<Object> inherited = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        runOnNewDuplicatedContext(() -> {
            MDC.put("requestId", "some-request-id");
            Context nested = newParentLinkedContext(Vertx.currentContext());
            nested.runOnContext(v -> {
                inherited.set(MDC.get("requestId"));
                latch.countDown();
            });
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("some-request-id", inherited.get());
    }

    @Test
    void parentLinkedContextInheritsMdcThroughIntermediateContext() throws InterruptedException {
        AtomicReference<Object> inherited = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        runOnNewDuplicatedContext(() -> {
            MDC.put("requestId", "grandparent-request-id");
            // the intermediate context never touches the MDC, so it has no materialized MDC map
            Context intermediate = newParentLinkedContext(Vertx.currentContext());
            intermediate.runOnContext(v -> {
                Context nested = newParentLinkedContext(Vertx.currentContext());
                nested.runOnContext(v2 -> {
                    inherited.set(MDC.get("requestId"));
                    latch.countDown();
                });
            });
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("grandparent-request-id", inherited.get());
    }

    @Test
    void parentLinkedContextMdcIsIsolatedFromParent() throws InterruptedException {
        AtomicReference<Object> parentValueAfterNestedWrite = new AtomicReference<>();
        AtomicReference<Object> parentOwnValue = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        runOnNewDuplicatedContext(() -> {
            MDC.put("requestId", "parent-request-id");
            Context parent = Vertx.currentContext();
            Context nested = newParentLinkedContext(parent);
            nested.runOnContext(v -> {
                MDC.put("nestedOnly", "nested-value");
                MDC.put("requestId", "overwritten-by-nested");
                parent.runOnContext(v2 -> {
                    parentValueAfterNestedWrite.set(MDC.get("nestedOnly"));
                    parentOwnValue.set(MDC.get("requestId"));
                    latch.countDown();
                });
            });
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(parentValueAfterNestedWrite.get(), "MDC writes on a nested context must not leak to the parent");
        assertEquals("parent-request-id", parentOwnValue.get(), "MDC writes on a nested context must not leak to the parent");
    }

    /**
     * Creates a per-invocation context the same way the REST Client does: a fresh duplicate of the
     * root context (so no Vert.x context locals are propagated) with a reference to the context the
     * invocation originates from.
     */
    private static Context newParentLinkedContext(Context current) {
        Context nested = VertxContext.createNewDuplicatedContext(current);
        ((ContextInternal) nested).putLocal(VertxContext.PARENT_CONTEXT_LOCAL, current);
        return nested;
    }

    private void runOnNewDuplicatedContext(Runnable runnable) {
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        duplicated.runOnContext(v -> runnable.run());
    }
}
