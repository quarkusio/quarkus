package io.quarkus.arc.test.event.hierarchy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class EventHierarchyCompatibilityTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Emitter.class, Observer.class, ListObserver.class);

    @Test
    public void resolvesTypeForSynchronousAndAsynchronousObservers() throws Exception {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        Observer observer = Arc.container().instance(Observer.class).get();
        emitter.typed.fire(new Payload<>("sync"));
        emitter.typed.fireAsync(new Payload<>("async")).toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertThat(observer.values).containsExactly("sync:sync", "async:async");
        assertThat(observer.types).hasSize(2).allSatisfy(type -> {
            assertThat(type).isInstanceOf(ParameterizedType.class);
            ParameterizedType parameterized = (ParameterizedType) type;
            assertThat(parameterized.getRawType()).isEqualTo(Payload.class);
            assertThat(parameterized.getActualTypeArguments()).containsExactly(String.class);
        });
    }

    @Test
    public void selectedTypeWithTypeVariableFailsDuringSelection() {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        selectWithTypeVariable(emitter.event);
    }

    private <T> void selectWithTypeVariable(Event<Object> event) {
        assertThatThrownBy(() -> event.select(new TypeLiteral<Payload<T>>() {
        }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void unresolvablePayloadFailsWhenFired() {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        Event<Object> selected = emitter.event.select(Object.class);
        assertThatThrownBy(() -> selected.fire(new Payload<>("sync")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> selected.fireAsync(new Payload<>("async")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void concreteSubtypeSelectionDelivers() {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        Observer observer = Arc.container().instance(Observer.class).get();
        emitter.event.select(ConcretePayload.class).fire(new ConcretePayload("subtype"));
        assertThat(observer.values).containsExactly("sync:subtype");
    }

    @Test
    public void infersRuntimeSubtypeFromSelectedSupertype() {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        ListObserver observer = Arc.container().instance(ListObserver.class).get();
        emitter.lists.fire(new ArrayList<>(List.of("value")));
        assertThat(observer.values).containsExactly("value");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void missingGenericDependencyFailsDuringSelection() throws Exception {
        Class<?> broken = new MissingTypeLoader().loadClass("io.quarkus.arc.test.event.hierarchy.fixture.Broken");
        Event event = Arc.container().beanManager().getEvent();
        assertThatThrownBy(() -> event.select(broken)).isInstanceOf(TypeNotPresentException.class);
    }

    @Test
    public void infersSeveralRuntimeSubtypesAfterFirstDelivery() {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        ListObserver observer = Arc.container().instance(ListObserver.class).get();
        emitter.lists.fire(new ArrayList<>(List.of("value")));
        emitter.lists.fire(new LinkedList<>(List.of("value")));
        emitter.lists.fire(new Vector<>(List.of("value")));
        emitter.lists.fire(new CopyOnWriteArrayList<>(List.of("value")));
        assertThat(observer.count.get()).isEqualTo(4);
        assertThat(observer.values).containsExactly("value");
    }

    @Test
    public void infersMultipleRuntimeSubtypesConcurrently() throws Exception {
        Emitter emitter = Arc.container().instance(Emitter.class).get();
        ListObserver observer = Arc.container().instance(ListObserver.class).get();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int index = i;
                tasks.add(executor.submit(() -> {
                    start.await();
                    for (int j = 0; j < 50; j++) {
                        List<String> list = index % 2 == 0 ? new ArrayList<>() : new LinkedList<>();
                        list.add("value");
                        emitter.lists.fire(list);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(30, TimeUnit.SECONDS);
            }
        }
        assertThat(observer.count.get()).isEqualTo(threads * 50);
    }

    static class MissingTypeLoader extends ClassLoader {
        private static final String PREFIX = "io.quarkus.arc.test.event.hierarchy.fixture.";

        MissingTypeLoader() {
            super(EventHierarchyCompatibilityTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(PREFIX + "Missing")) {
                throw new ClassNotFoundException(name);
            }
            if (name.equals(PREFIX + "Broken") || name.equals(PREFIX + "Parent")) {
                Class<?> result = findLoadedClass(name);
                if (result == null) {
                    try (InputStream stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = stream.readAllBytes();
                        result = defineClass(name, bytes, 0, bytes.length);
                    } catch (Exception e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                if (resolve) {
                    resolveClass(result);
                }
                return result;
            }
            return super.loadClass(name, resolve);
        }
    }

    static class Payload<T> {
        final T value;

        Payload(T value) {
            this.value = value;
        }
    }

    static class ConcretePayload extends Payload<String> {
        ConcretePayload(String value) {
            super(value);
        }
    }

    @Singleton
    static class Emitter {
        @Inject
        Event<Payload<String>> typed;
        @Inject
        Event<Object> event;
        @Inject
        Event<List<String>> lists;
    }

    @Singleton
    static class ListObserver {
        volatile List<String> values;
        final AtomicInteger count = new AtomicInteger();

        void observe(@Observes List<String> payload) {
            values = payload;
            count.incrementAndGet();
        }
    }

    @Singleton
    static class Observer {
        final List<String> values = new CopyOnWriteArrayList<>();
        final List<Type> types = new CopyOnWriteArrayList<>();

        void sync(@Observes Payload<String> payload, EventMetadata metadata) {
            values.add("sync:" + payload.value);
            types.add(metadata.getType());
        }

        void async(@ObservesAsync Payload<String> payload, EventMetadata metadata) {
            values.add("async:" + payload.value);
            types.add(metadata.getType());
        }
    }
}
