package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.entitylistener;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ReceivedEvent {
    public static String objectRef(Class<?> objectType, Integer objectId) {
        return objectType.getSimpleName() + "#" + objectId;
    }

    private static final Map<String, List<ReceivedEvent>> all = new ConcurrentHashMap<>();

    public static Map<String, List<ReceivedEvent>> get() {
        return all;
    }

    public static void clear() {
        all.clear();
    }

    public static void add(String listenerRef, ReceivedEvent event) {
        all.computeIfAbsent(listenerRef, ignored -> new ArrayList<>()).add(event);
    }

    private final Class<? extends Annotation> eventType;
    private final String entityRef;

    public ReceivedEvent(Class<? extends Annotation> eventType, String entityRef) {
        this.eventType = eventType;
        this.entityRef = entityRef;
    }

    @Override
    public String toString() {
        return "ReceivedEvent{" +
                "eventType=" + eventType +
                ", entityRef='" + entityRef + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReceivedEvent that = (ReceivedEvent) o;
        return Objects.equals(eventType, that.eventType) && Objects.equals(entityRef, that.entityRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, entityRef);
    }
}
