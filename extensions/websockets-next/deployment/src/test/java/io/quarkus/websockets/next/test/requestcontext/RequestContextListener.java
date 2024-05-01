package io.quarkus.websockets.next.test.requestcontext;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Singleton;

@Singleton
public class RequestContextListener {

    final List<ContextEvent> events = new CopyOnWriteArrayList<>();

    void clear() {
        events.clear();
    }

    void init(@Observes @Initialized(RequestScoped.class) Object event, EventMetadata metadata) {
        events.add(new ContextEvent(metadata.getQualifiers(), event.toString()));
    }

    void beforeDestroy(@Observes @BeforeDestroyed(RequestScoped.class) Object event, EventMetadata metadata) {
        events.add(new ContextEvent(metadata.getQualifiers(), event.toString()));
    }

    void destroy(@Observes @Destroyed(RequestScoped.class) Object event, EventMetadata metadata) {
        events.add(new ContextEvent(metadata.getQualifiers(), event.toString()));
    }

    record ContextEvent(Set<Annotation> qualifiers, String payload) {
    }

}
