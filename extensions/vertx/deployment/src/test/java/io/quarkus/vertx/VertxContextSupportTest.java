package io.quarkus.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class VertxContextSupportTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Alpha.class));

    @Inject
    Alpha alpha;

    @Test
    public void testRunner() throws InterruptedException {
        assertEquals("foo", alpha.val);
        assertTrue(alpha.latch.await(5, TimeUnit.SECONDS));
        assertEquals(5, alpha.vals.size());
        assertEquals(1, alpha.vals.get(0));
    }

    @Singleton
    public static class Alpha {

        String val;

        final List<Integer> vals = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        void onStart(@Observes StartupEvent event) {
            Supplier<Uni<String>> uniSupplier = new Supplier<Uni<String>>() {
                @Override
                public Uni<String> get() {
                    assertTrue(VertxContext.isOnDuplicatedContext());
                    VertxContextSafetyToggle.validateContextIfExists("Error", "Error");
                    assertTrue(Arc.container().requestContext().isActive());
                    return Uni.createFrom().item("foo");
                }
            };
            try {
                val = VertxContextSupport.subscribeAndAwait(uniSupplier);
            } catch (Throwable e) {
                fail();
            }

            Supplier<Multi<Integer>> multiSupplier = new Supplier<Multi<Integer>>() {

                @Override
                public Multi<Integer> get() {
                    assertTrue(VertxContext.isOnDuplicatedContext());
                    VertxContextSafetyToggle.validateContextIfExists("Error", "Error");
                    return Multi.createFrom().items(1, 2, 3, 4, 5);
                }
            };
            VertxContextSupport.subscribe(multiSupplier, ms -> ms.with(vals::add, latch::countDown));
        }
    }

}
