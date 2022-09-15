package io.quarkus.arc.test.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ParameterizedPayloadTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ListObserver.class, ListProducer.class);

    @Test
    public void testObservers() {
        ListProducer producer = Arc.container().instance(ListProducer.class).get();
        ListObserver observer = Arc.container().instance(ListObserver.class).get();
        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        producer.produceInt(intList);
        List<? extends Number> observedInt = observer.getIntList();
        assertNotNull(observedInt);
        assertEquals(1, observedInt.size());
        assertEquals(1, observedInt.get(0));

        List<String> strList = new ArrayList<>();
        strList.add("ping");
        producer.produceStr(strList);
        List<String> observedStr = observer.getStrList();
        assertNotNull(observedStr);
        assertEquals(1, observedStr.size());
        assertEquals("ping", observedStr.get(0));
    }

    @Singleton
    static class ListObserver {

        private AtomicReference<List<? extends Number>> intList;

        private AtomicReference<List<String>> strList;

        @PostConstruct
        void init() {
            intList = new AtomicReference<>();
            strList = new AtomicReference<>();
        }

        <T extends List<? extends Number>> void observeIntList(@Observes T value) {
            intList.set(value);
        }

        List<? extends Number> getIntList() {
            return intList.get();
        }

        void observeStrList(@Observes List<String> value) {
            strList.set(value);
        }

        List<String> getStrList() {
            return strList.get();
        }

    }

    @Dependent
    static class ListProducer {

        @Inject
        Event<List<Integer>> intEvent;

        @Inject
        Event<Collection<String>> strEvent;

        void produceInt(List<Integer> value) {
            intEvent.fire(value);
        }

        void produceStr(Collection<String> value) {
            strEvent.fire(value);
        }

    }

}
