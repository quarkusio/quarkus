package io.quarkus.arc.runtime.devconsole;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Singleton;

@Singleton
public class EventsMonitor {

    private static final int DEFAULT_LIMIT = 500;

    private volatile boolean skipContextEvents = true;
    private final List<EventInfo> events = Collections.synchronizedList(new ArrayList<EventInfo>(DEFAULT_LIMIT));

    void notify(@Observes Object payload, EventMetadata eventMetadata) {
        if (skipContextEvents && isContextEvent(eventMetadata)) {
            return;
        }
        if (events.size() > DEFAULT_LIMIT) {
            // Remove some old data if the limit is exceeded
            synchronized (events) {
                if (events.size() > DEFAULT_LIMIT) {
                    events.subList(0, DEFAULT_LIMIT / 2).clear();
                }
            }
        }
        events.add(EventInfo.from(eventMetadata));
    }

    public void clear() {
        events.clear();
    }

    public List<EventInfo> getLastEvents() {
        List<EventInfo> result = new ArrayList<>(events);
        Collections.reverse(result);
        return result;
    }

    public boolean isSkipContextEvents() {
        return skipContextEvents;
    }

    public void toggleSkipContextEvents() {
        // This is not thread-safe but we don't expect concurrent actions from dev ui
        skipContextEvents = !skipContextEvents;
    }

    boolean isContextEvent(EventMetadata eventMetadata) {
        if (!eventMetadata.getType().equals(Object.class) || eventMetadata.getQualifiers().size() != 2) {
            return false;
        }
        for (Annotation qualifier : eventMetadata.getQualifiers()) {
            Type qualifierType = qualifier.annotationType();
            // @Any, @Initialized, @BeforeDestroyed or @Destroyed
            if (!qualifierType.equals(Any.class) && !qualifierType.equals(Initialized.class)
                    && !qualifierType.equals(BeforeDestroyed.class) && !qualifierType.equals(Destroyed.class)) {

                return false;
            }
        }
        return true;
    }

    static class EventInfo {

        static EventInfo from(EventMetadata eventMetadata) {
            List<Annotation> qualifiers;
            if (eventMetadata.getQualifiers().size() == 1) {
                // Just @Any
                qualifiers = Collections.emptyList();
            } else {
                qualifiers = new ArrayList<>(1);
                for (Annotation qualifier : eventMetadata.getQualifiers()) {
                    // Skip @Any and @Default
                    if (!qualifier.annotationType().equals(Any.class) && !qualifier.annotationType().equals(Default.class)) {
                        qualifiers.add(qualifier);
                    }
                }
            }
            return new EventInfo(eventMetadata.getType(), qualifiers);
        }

        private final LocalDateTime timestamp;
        private final Type type;
        private final List<Annotation> qualifiers;

        EventInfo(Type type, List<Annotation> qualifiers) {
            this.timestamp = LocalDateTime.now();
            this.type = type;
            this.qualifiers = qualifiers;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getType() {
            return type.getTypeName();
        }

        public List<Annotation> getQualifiers() {
            return qualifiers;
        }

    }

}
