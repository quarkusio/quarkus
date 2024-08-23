package io.quarkus.arc.test.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ArrayPayloadTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Observer.class, Emitter.class);

    @Test
    public void testObservers() {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        Observer observer = Arc.container().instance(Observer.class).get();

        Integer[] integerArray = new Integer[] { 42 };
        emitter.emitIntegerArray(integerArray);
        Integer[] observedIntegerArray = observer.integerArray.get();
        assertNotNull(observedIntegerArray);
        assertEquals(1, observedIntegerArray.length);
        assertEquals(42, observedIntegerArray[0]);

        List<?>[] listArray = new List[] { List.of(42) };
        emitter.emitListArray(listArray);
        List<?>[] observedListArray = observer.listArray.get();
        assertNotNull(observedListArray);
        assertEquals(1, observedListArray.length);
        assertEquals(List.of(42), observedListArray[0]);
    }

    @Singleton
    static class Observer {
        AtomicReference<Integer[]> integerArray = new AtomicReference<>();
        AtomicReference<List<?>[]> listArray = new AtomicReference<>();

        <T extends Integer> void observeIntegerArray(@Observes T[] value) {
            integerArray.set(value);
        }

        void observeListArray(@Observes List<?>[] value) {
            listArray.set(value);
        }
    }

    @Dependent
    static class Emitter {
        @Inject
        Event<Integer[]> integerArrayEvent;

        @Inject
        Event<List<?>[]> listArrayEvent;

        void emitIntegerArray(Integer[] value) {
            integerArrayEvent.fire(value);
        }

        void emitListArray(List<?>[] value) {
            listArrayEvent.fire(value);
        }
    }
}
