package org.jboss.protean.arc.test.contexts.application;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ApplicationInitializedTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Observer.class);

    @Test
    public void testEventWasFired() {
        Assert.assertTrue(Observer.OBSERVED.get());
    }

    @Singleton
    static class Observer {

        static final AtomicBoolean OBSERVED = new AtomicBoolean(false);

        void onStart(@Observes @Initialized(ApplicationScoped.class) Object container) {
            OBSERVED.set(true);
        }

    }
}
