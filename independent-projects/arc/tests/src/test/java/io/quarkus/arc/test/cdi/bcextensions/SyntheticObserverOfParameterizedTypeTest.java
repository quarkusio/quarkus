package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticObserverOfParameterizedTypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyService myService = Arc.container().select(MyService.class).get();
        myService.fireEvent();

        assertIterableEquals(List.of("Hello World", "Hello again"), MyObserver.observed);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        private final List<ObserverInfo> observers = new ArrayList<>();

        @Registration(types = List.class)
        public void rememberObservers(ObserverInfo observer) {
            observers.add(observer);
        }

        @Synthesis
        public void synthesise(SyntheticComponents syn, Types types) {
            syn.<List<MyData>> addObserver(types.parameterized(List.class, MyData.class))
                    .observeWith(MyObserver.class);
        }

        @Validation
        public void validate(Messages messages) {
            for (ObserverInfo observer : observers) {
                messages.info("observer has type " + observer.eventType(), observer);
            }
        }
    }

    // ---

    static class MyData {
        final String payload;

        MyData(String payload) {
            this.payload = payload;
        }
    }

    @Singleton
    static class MyService {
        @Inject
        Event<List<MyData>> event;

        void fireEvent() {
            event.fire(List.of(new MyData("Hello"), new MyData("World")));
            event.fire(List.of(new MyData("Hello"), new MyData("again")));
        }
    }

    static class MyObserver implements SyntheticObserver<List<MyData>> {
        static final List<String> observed = new ArrayList<>();

        @Override
        public void observe(EventContext<List<MyData>> event, Parameters params) throws Exception {
            observed.add(event.getEvent()
                    .stream()
                    .map(it -> it.payload)
                    .collect(Collectors.joining(" ")));
        }
    }
}
