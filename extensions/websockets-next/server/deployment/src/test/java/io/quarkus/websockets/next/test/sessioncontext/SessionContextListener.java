package io.quarkus.websockets.next.test.sessioncontext;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Singleton;

@Singleton
public class SessionContextListener {

    final CountDownLatch destroyLatch = new CountDownLatch(1);
    final List<ContextEvent> events = new CopyOnWriteArrayList<>();

    void init(@Observes @Initialized(SessionScoped.class) Object event, EventMetadata metadata) {
        events.add(new ContextEvent(metadata.getQualifiers(), event.toString()));
    }

    void beforeDestroy(@Observes @BeforeDestroyed(SessionScoped.class) Object event, EventMetadata metadata) {
        events.add(new ContextEvent(metadata.getQualifiers(), event.toString()));
    }

    void destroy(@Observes @Destroyed(SessionScoped.class) Object event, EventMetadata metadata) {
        events.add(new ContextEvent(metadata.getQualifiers(), event.toString()));
        destroyLatch.countDown();
    }

    record ContextEvent(Set<Annotation> qualifiers, String payload) {
    };

}
