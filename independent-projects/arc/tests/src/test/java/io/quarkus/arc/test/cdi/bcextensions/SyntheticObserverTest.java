package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticObserverTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyService.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyService myService = Arc.container().select(MyService.class).get();
        myService.fireEvent();

        List<String> expected = List.of(
                "Hello World with null", // unqualified event observed by unqualified observer
                "Hello SynObserver with null", // qualified event observed by unqualified observer
                "Hello SynObserver with @MyQualifier" // qualified event observed by qualified observer
        );
        assertIterableEquals(expected, MyObserver.observed);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        private final List<ObserverInfo> observers = new ArrayList<>();

        @Registration(types = MyEvent.class)
        public void rememberObservers(ObserverInfo observer) {
            observers.add(observer);
        }

        @Synthesis
        public void synthesise(SyntheticComponents syn) {
            syn.addObserver(MyEvent.class)
                    .priority(10)
                    .observeWith(MyObserver.class);

            syn.addObserver(MyEvent.class)
                    .qualifier(MyQualifier.class)
                    .priority(20)
                    .withParam("name", "@MyQualifier")
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

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
    }

    static class MyEvent {
        final String payload;

        MyEvent(String payload) {
            this.payload = payload;
        }
    }

    @Singleton
    static class MyService {
        @Inject
        Event<MyEvent> unqualifiedEvent;

        @Inject
        @MyQualifier
        Event<MyEvent> qualifiedEvent;

        void fireEvent() {
            unqualifiedEvent.fire(new MyEvent("Hello World"));
            qualifiedEvent.fire(new MyEvent("Hello SynObserver"));
        }
    }

    static class MyObserver implements SyntheticObserver<MyEvent> {
        static final List<String> observed = new ArrayList<>();

        @Override
        public void observe(EventContext<MyEvent> event, Parameters params) throws Exception {
            observed.add(event.getEvent().payload + " with " + params.get("name", String.class));
        }
    }
}
