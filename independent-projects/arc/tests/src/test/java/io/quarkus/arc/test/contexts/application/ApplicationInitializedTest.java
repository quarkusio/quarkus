package io.quarkus.arc.test.contexts.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ApplicationInitializedTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Observer.class);

    @Test
    public void testEventWasFired() {
        assertTrue(Observer.INITIALIZED.get());
    }

    @AfterAll
    public static void afterAll() {
        assertTrue(Observer.DESTROYED.get());
        assertTrue(Observer.BEFORE_DESTROYED.get());
    }

    @Dependent
    static class Observer {

        static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);
        static final AtomicBoolean BEFORE_DESTROYED = new AtomicBoolean(false);

        void onStart(@Observes @Initialized(ApplicationScoped.class) Object container) {
            INITIALIZED.set(true);
        }

        void beforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object container) {
            BEFORE_DESTROYED.set(true);
        }

        void destroyed(@Observes @Destroyed(ApplicationScoped.class) Object container) {
            DESTROYED.set(true);
        }

    }
}
