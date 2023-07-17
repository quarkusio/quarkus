package io.quarkus.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import io.smallrye.mutiny.Uni;

public class VertxContextSupportTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Alpha.class));

    @Inject
    Alpha alpha;

    @Test
    public void testRunner() {
        assertEquals("foo", alpha.val);
    }

    @Singleton
    public static class Alpha {

        String val;

        void onStart(@Observes StartupEvent event) {
            Supplier<Uni<String>> supplier = new Supplier<Uni<String>>() {
                @Override
                public Uni<String> get() {
                    assertTrue(VertxContext.isOnDuplicatedContext());
                    VertxContextSafetyToggle.validateContextIfExists("Error", "Error");
                    assertTrue(Arc.container().requestContext().isActive());
                    return Uni.createFrom().item("foo");
                }
            };
            try {
                val = VertxContextSupport.subscribeAndAwait(supplier);
            } catch (Throwable e) {
                fail();
            }
        }
    }

}
