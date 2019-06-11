package io.quarkus.arc.test.beanmanager;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import org.junit.Rule;
import org.junit.Test;

public class BeanManagerEventTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testGetEvent() {
        BeanManager beanManager = Arc.container().beanManager();
        beanManager.getEvent().fire("foo");
        assertEquals("foo", StringObserver.OBSERVED.get());
    }

    @Dependent
    static class StringObserver {

        private static final AtomicReference<String> OBSERVED = new AtomicReference<String>();

        void observeString(@Observes String value) {
            OBSERVED.set(value);
        }

    }

}
