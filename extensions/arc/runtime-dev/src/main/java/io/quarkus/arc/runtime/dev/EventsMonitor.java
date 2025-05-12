package io.quarkus.arc.runtime.dev;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@Singleton
public class EventsMonitor {

    private static final int DEFAULT_LIMIT = 500;

    private volatile boolean skipContextEvents = true;
    private final List<EventInfo> events = Collections.synchronizedList(new ArrayList<>(DEFAULT_LIMIT));
    private final BroadcastProcessor<EventInfo> eventsStream = BroadcastProcessor.create();
    private final BroadcastProcessor<Boolean> skipContextEventsStream = BroadcastProcessor.create();

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
        EventInfo eventInfo = toEventInfo(payload, eventMetadata);
        eventsStream.onNext(eventInfo);
        events.add(eventInfo);
    }

    public void clear() {
        events.clear();
    }

    public Multi<EventInfo> streamEvents() {
        return eventsStream;
    }

    public Multi<Boolean> streamSkipContextEvents() {
        return skipContextEventsStream;
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
        skipContextEventsStream.onNext(skipContextEvents);
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

    private EventInfo toEventInfo(Object payload, EventMetadata eventMetadata) {
        EventInfo eventInfo = new EventInfo();
        eventInfo.setTimestamp(now());
        eventInfo.setType(eventMetadata.getType().getTypeName());
        List<String> q = new ArrayList<>();
        if (eventMetadata.getQualifiers().size() > 1) {
            for (Annotation qualifier : eventMetadata.getQualifiers()) {
                // Skip @Any and @Default
                if (!qualifier.annotationType().equals(Any.class) && !qualifier.annotationType().equals(Default.class)) {
                    q.add(qualifier.toString());
                }
            }
        }
        eventInfo.setQualifiers(q);
        eventInfo.setIsContextEvent(isContextEvent(eventMetadata));
        return eventInfo;
    }

    private String now() {
        LocalDateTime time = LocalDateTime.now();
        String timestamp = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ");
        return timestamp.substring(0, timestamp.lastIndexOf("."));
    }

}
