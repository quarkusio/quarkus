package io.quarkus.arc.test.contexts.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.arc.impl.Reflections;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * A container that fails to initialize is never shut down. It must not stay installed, and its classes must not stay
 * reachable from the static caches, because the application may be started again in the same JVM (dev mode retries a
 * failed start on the next request).
 */
public class ApplicationInitializedFailureTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Holder.class, Dependency.class, FailingObserver.class)
            .shouldFail()
            .build();

    @Test
    public void testFailedContainerIsReleased() throws ReflectiveOperationException {
        Throwable failure = container.getFailure();
        assertNotNull(failure);
        assertTrue(hasCause(failure, "boom"), () -> "Unexpected failure: " + failure);
        assertNull(Arc.container());
        assertTrue(fieldsCache().isEmpty());
    }

    private static boolean hasCause(Throwable failure, String message) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (message.equals(t.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private static ComputingCache<?, ?> fieldsCache() throws ReflectiveOperationException {
        Field field = Reflections.class.getDeclaredField("FIELDS_CACHE");
        field.setAccessible(true);
        return (ComputingCache<?, ?>) field.get(null);
    }

    @Singleton
    static class Holder {

        // the InjectionPoint metadata of this field holds its java.lang.reflect.Field, found through Reflections
        @Inject
        Dependency dependency;

    }

    @Dependent
    static class Dependency {

        @Inject
        InjectionPoint injectionPoint;

    }

    @Dependent
    static class FailingObserver {

        void onStart(@Observes @Initialized(ApplicationScoped.class) Object container, Holder holder) {
            throw new IllegalStateException("boom");
        }

    }
}
