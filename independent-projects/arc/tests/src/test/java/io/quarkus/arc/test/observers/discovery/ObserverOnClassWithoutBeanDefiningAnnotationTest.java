package io.quarkus.arc.test.observers.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ObserverOnClassWithoutBeanDefiningAnnotationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testObserver() {
        BeanManager beanManager = Arc.container().beanManager();
        beanManager.getEvent().fire("foo");
        beanManager.getEvent().fire("ping");
        assertEquals(2, StringObserver.EVENTS.size());
    }

    static class StringObserver {

        private static List<String> EVENTS = new CopyOnWriteArrayList<>();

        void observeString(@Observes String value) {
            EVENTS.add(value);
        }

    }

}
