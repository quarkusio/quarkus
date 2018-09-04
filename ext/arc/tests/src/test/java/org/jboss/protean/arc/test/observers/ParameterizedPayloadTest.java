package org.jboss.protean.arc.test.observers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ParameterizedPayloadTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(ListObserver.class, ListProducer.class);

    @Test
    public void testObserver() {
        ListProducer producer = Arc.container().instance(ListProducer.class).get();
        ListObserver observer = Arc.container().instance(ListObserver.class).get();
        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        producer.produce(intList);
        List<? extends Number> observed = observer.getList();
        assertNotNull(observed);
        assertEquals(1, observed.size());
        assertEquals(1, observed.get(0));
    }

    @Singleton
    static class ListObserver {

        private AtomicReference<List<? extends Number>> list;

        @PostConstruct
        void init() {
            list = new AtomicReference<>();
        }

        <T extends List<? extends Number>> void observeIntList(@Observes T value) {
            list.set(value);
        }

        List<? extends Number> getList() {
            return list.get();
        }

    }

    @Dependent
    static class ListProducer {

        @Inject
        Event<List<Integer>> event;

        void produce(List<Integer> value) {
            event.fire(value);
        }

    }

}
