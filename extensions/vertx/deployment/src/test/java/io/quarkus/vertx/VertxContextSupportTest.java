package io.quarkus.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.BlockingOperationControl;
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

        @Inject
        Bravo bravo;

        String val;

        final List<Integer> vals = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        void onStart(@Observes StartupEvent event) {
            // Request context is active but duplicated context is not used
            String bravoId = bravo.getId();
            Supplier<Uni<String>> uniSupplier = new Supplier<Uni<String>>() {
                @Override
                public Uni<String> get() {
                    assertTrue(VertxContext.isOnDuplicatedContext());
                    VertxContextSafetyToggle.validateContextIfExists("Error", "Error");
                    assertTrue(Arc.container().requestContext().isActive());
                    // New duplicated contex -> new request context
                    String asyncBravoId = bravo.getId();
                    assertNotEquals(bravoId, asyncBravoId);

                    return VertxContextSupport.executeBlocking(() -> {
                        assertTrue(BlockingOperationControl.isBlockingAllowed());
                        assertTrue(VertxContext.isOnDuplicatedContext());
                        assertTrue(Arc.container().requestContext().isActive());
                        // Duplicated context is propagated -> the same request context
                        assertEquals(asyncBravoId, bravo.getId());
                        return "foo";
                    });
                }
            };
            try {
                val = VertxContextSupport.subscribeAndAwait(uniSupplier);
            } catch (Throwable e) {
                fail(e);
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

    @RequestScoped
    public static class Bravo {

        private String id;

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

        public String getId() {
            return id;
        }

    }

}
