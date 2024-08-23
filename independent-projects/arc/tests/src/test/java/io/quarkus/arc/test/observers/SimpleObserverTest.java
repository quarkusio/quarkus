package io.quarkus.arc.test.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SimpleObserverTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class);

    @Test
    public void testObserver() {
        StringProducer producer = Arc.container().instance(StringProducer.class).get();
        StringObserver observer = Arc.container().instance(StringObserver.class).get();
        producer.produce("foo");
        producer.produce("ping");
        List<String> events = observer.getEvents();
        assertEquals(2, events.size());

        // verify we can resolve OM and check some of its metadata
        Set<ObserverMethod<? super String>> foundOms = Arc.container().beanManager().resolveObserverMethods("someString");
        assertEquals(1, foundOms.size());
        ObserverMethod<? super String> om = foundOms.iterator().next();
        Bean<?> declaringBean = om.getDeclaringBean();
        assertNotNull(declaringBean);
        assertEquals(StringObserver.class, declaringBean.getBeanClass());
        assertEquals(Singleton.class, declaringBean.getScope());
    }

    @Singleton
    static class StringObserver {

        private List<String> events;

        @PostConstruct
        void init() {
            events = new CopyOnWriteArrayList<>();
        }

        void observeString(@Observes String value) {
            events.add(value);
        }

        List<String> getEvents() {
            return events;
        }

    }

    @Dependent
    static class StringProducer {

        @Inject
        Event<String> event;

        void produce(String value) {
            event.fire(value);
        }

    }

}
