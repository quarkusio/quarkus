package io.quarkus.arc.impl;

import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.InjectionPoint;

public class EventBean extends BuiltInBean<Event<?>> {

    public static final Set<Type> EVENT_TYPES = Set.of(Event.class, Object.class);

    @Override
    public Set<Type> getTypes() {
        return EVENT_TYPES;
    }

    @Override
    public Event<?> get(CreationalContext<Event<?>> creationalContext) {
        // Obtain current IP to get the required type and qualifiers
        InjectionPoint ip = InjectionPointProvider.get();
        return new EventImpl<>(ip.getType(), ip.getQualifiers());
    }

    @Override
    public Class<?> getBeanClass() {
        return EventImpl.class;
    }
}
