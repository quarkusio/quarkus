package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ChangeObserverQualifierTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyConsumer.class, MyProducer.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyProducer myProducer = Arc.container().select(MyProducer.class).get();
        myProducer.produce();
        assertEquals(Set.of("qualified"), MyConsumer.events);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Enhancement(types = MyConsumer.class)
        public void consumer(MethodConfig method) {
            switch (method.info().name()) {
                case "consume":
                    method.parameters().get(0).addAnnotation(MyQualifier.class);
                    break;
                case "noConsume":
                    method.parameters().get(0).removeAllAnnotations();
                    break;
            }
        }
    }

    // ---

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
    }

    static class MyEvent {
        String payload;

        MyEvent(String payload) {
            this.payload = payload;
        }
    }

    @Singleton
    static class MyConsumer {
        static final Set<String> events = new HashSet<>();

        void consume(@Observes MyEvent event) {
            events.add(event.payload);
        }

        void noConsume(@Observes MyEvent event) {
            events.add("must-not-happen-" + event.payload);
        }
    }

    @Singleton
    static class MyProducer {
        @Inject
        Event<MyEvent> unqualified;

        @Inject
        @MyQualifier
        Event<MyEvent> qualified;

        void produce() {
            unqualified.fire(new MyEvent("unqualified"));
            qualified.fire(new MyEvent("qualified"));
        }
    }
}
