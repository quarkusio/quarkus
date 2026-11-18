package io.quarkus.arc.test.event.select;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Observer resolution is cached, so each event is fired twice to make sure that the second notification still reaches
 * exactly the observers that match the selected type and qualifiers.
 */
public class RepeatedEventSelectTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Payload.class, Color.class, Producer.class,
            PayloadObserver.class);

    @Test
    public void testSelectByTypeLiteral() {
        Producer producer = Arc.container().instance(Producer.class).get();
        PayloadObserver observer = Arc.container().instance(PayloadObserver.class).get();

        for (int i = 0; i < 2; i++) {
            observer.notified.clear();
            producer.event.select(new TypeLiteral<Payload<String>>() {
            }).fire(new Payload<>("a"));
            // observers with smaller priority values are called first
            assertThat(observer.notified).containsExactly("first:a", "string:a");

            observer.notified.clear();
            producer.event.select(new TypeLiteral<Payload<Integer>>() {
            }).fire(new Payload<>(1));
            assertThat(observer.notified).containsExactly("integer:1");

            observer.notified.clear();
            producer.event.select(new TypeLiteral<Payload<Long>>() {
            }).fire(new Payload<>(1L));
            assertThat(observer.notified).isEmpty();
        }
    }

    @Test
    public void testSelectWithQualifiers() {
        Producer producer = Arc.container().instance(Producer.class).get();
        PayloadObserver observer = Arc.container().instance(PayloadObserver.class).get();

        for (int i = 0; i < 2; i++) {
            observer.notified.clear();
            producer.event.select(new TypeLiteral<Payload<String>>() {
            }, new Color.Literal("red")).fire(new Payload<>("b"));
            assertThat(observer.notified).containsExactly("first:b", "string:b", "red:b");

            observer.notified.clear();
            producer.event.select(new TypeLiteral<Payload<String>>() {
            }, new Color.Literal("blue")).fire(new Payload<>("c"));
            assertThat(observer.notified).containsExactly("first:c", "string:c", "blue:c");

            // the injected event is @Color("blue")
            observer.notified.clear();
            producer.blueEvent.select(new TypeLiteral<Payload<String>>() {
            }).fire(new Payload<>("d"));
            assertThat(observer.notified).containsExactly("first:d", "string:d", "blue:d");
        }
    }

    @Test
    public void testFireAsync() throws Exception {
        Producer producer = Arc.container().instance(Producer.class).get();
        PayloadObserver observer = Arc.container().instance(PayloadObserver.class).get();

        for (int i = 0; i < 2; i++) {
            observer.notified.clear();
            producer.event.select(new TypeLiteral<Payload<String>>() {
            }).fireAsync(new Payload<>("e")).toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertThat(observer.notified).containsExactly("async:e");
        }
    }

    static class Payload<T> {

        final T value;

        Payload(T value) {
            this.value = value;
        }
    }

    @Qualifier
    @Target({ FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Color {

        String value();

        @SuppressWarnings("all")
        class Literal extends AnnotationLiteral<Color> implements Color {

            private final String value;

            Literal(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    @Singleton
    static class Producer {

        @Inject
        Event<Object> event;

        @Inject
        @Color("blue")
        Event<Object> blueEvent;
    }

    @Singleton
    static class PayloadObserver {

        final List<String> notified = new CopyOnWriteArrayList<>();

        void string(@Observes Payload<String> payload) {
            notified.add("string:" + payload.value);
        }

        void first(@Observes @Priority(1) Payload<String> payload) {
            notified.add("first:" + payload.value);
        }

        void red(@Observes @Priority(3000) @Color("red") Payload<String> payload) {
            notified.add("red:" + payload.value);
        }

        void blue(@Observes @Priority(3000) @Color("blue") Payload<String> payload) {
            notified.add("blue:" + payload.value);
        }

        void integer(@Observes Payload<Integer> payload) {
            notified.add("integer:" + payload.value);
        }

        void async(@ObservesAsync Payload<String> payload) {
            notified.add("async:" + payload.value);
        }
    }
}
