package io.quarkus.vertx.arc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * This is part 2 of the same test file in the Quarkus Arc extension, because this depends on Vert.x, so we could
 * not put it there without a cyclic dependency.
 */
public class StartupAnnotationTest {

    static final List<String> LOG = new CopyOnWriteArrayList<String>();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ThreadedMethods.class, DependentStartMe.class));

    @Test
    public void testStartup() {
        assertEquals(4, LOG.size(), "Unexpected number of log messages: " + LOG);
        int order = 0;
        assertEquals("nonBlocking dependent, blocking allowed: false", LOG.get(order++));
        assertEquals("blocking dependent, blocking allowed: true", LOG.get(order++));
        assertEquals("nonBlocking, blocking allowed: false", LOG.get(order++));
        assertEquals("blocking, blocking allowed: true", LOG.get(order++));
    }

    static class ThreadedMethods {
        @Startup(Integer.MAX_VALUE - 30)
        void blocking() {
            LOG.add("blocking, blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
        }

        @Startup(Integer.MAX_VALUE - 31)
        Uni<Void> nonBlocking() {
            LOG.add("nonBlocking, blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
            return Uni.createFrom().voidItem();
        }
    }

    @Dependent
    static class DependentStartMe {
        @Startup(Integer.MAX_VALUE - 32)
        void blocking() {
            LOG.add("blocking dependent, blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
        }

        @Startup(Integer.MAX_VALUE - 33)
        Uni<Void> nonBlocking() {
            LOG.add("nonBlocking dependent, blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
            return Uni.createFrom().voidItem();
        }
    }
}
