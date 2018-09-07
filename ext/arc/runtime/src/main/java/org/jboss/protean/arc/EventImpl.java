package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.TypeLiteral;

/**
 * 
 * @author Martin Kouba
 *
 * @param <T>
 */
class EventImpl<T> implements Event<T> {

    private static final int DEFAULT_CACHE_CAPACITY = 4;

    private final HierarchyDiscovery injectionPointTypeHierarchy;

    private final Set<Annotation> qualifiers;

    private final ConcurrentMap<Class<?>, Notifier<? super T>> notifierCache;

    public EventImpl(Type eventType, Set<Annotation> qualifiers) {
        if (eventType instanceof ParameterizedType) {
            eventType = ((ParameterizedType) eventType).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException();
        }
        this.injectionPointTypeHierarchy = new HierarchyDiscovery(eventType);
        this.qualifiers = qualifiers;
        this.qualifiers.add(Any.Literal.INSTANCE);
        this.notifierCache = new ConcurrentHashMap<>(DEFAULT_CACHE_CAPACITY);
    }

    @Override
    public void fire(T event) {
        notifierCache.computeIfAbsent(event.getClass(), this::createNotifier).notify(event);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Event<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    private Notifier<? super T> createNotifier(Class<?> runtimeType) {
        Type eventType = getEventType(runtimeType);
        EventMetadata metadata = new EventMetadataImpl(qualifiers, eventType);
        List<ObserverMethod<? super T>> notifierObserverMethods = new ArrayList<>();
        for (ObserverMethod<? super T> observerMethod : ArcContainerImpl.unwrap(Arc.container()).resolveObservers(eventType, qualifiers)) {
            if (EventTypeAssignabilityRules.matches(observerMethod.getObservedType(), eventType)) {
                notifierObserverMethods.add(observerMethod);
            }
        }
        return new Notifier<>(notifierObserverMethods, metadata);
    }
    
    static <T> Notifier<T> createNotifier(Type eventType, Set<Annotation> qualifiers, ArcContainerImpl container) {
        EventMetadata metadata = new EventMetadataImpl(qualifiers, eventType);
        List<ObserverMethod<? super T>> notifierObserverMethods = new ArrayList<>();
        for (ObserverMethod<? super T> observerMethod : container.resolveObservers(eventType, qualifiers)) {
            if (EventTypeAssignabilityRules.matches(observerMethod.getObservedType(), eventType)) {
                notifierObserverMethods.add(observerMethod);
            }
        }
        return new Notifier<>(notifierObserverMethods, metadata);
    }

    private Type getEventType(Class<?> runtimeType) {
        Type resolvedType = runtimeType;
        if (Types.containsTypeVariable(resolvedType)) {
            /*
             * If the container is unable to resolve the parameterized type of the event object, it uses the specified type to infer the parameterized type of
             * the event types.
             */
            resolvedType = injectionPointTypeHierarchy.resolveType(resolvedType);
        }
        if (Types.containsTypeVariable(resolvedType)) {
            /*
             * Examining the hierarchy of the specified type did not help. This may still be one of the cases when combining the event type and the specified
             * type reveals the actual values for type variables. Let's try that.
             */
            Type canonicalEventType = Types.getCanonicalType(runtimeType);
            TypeResolver objectTypeResolver = new EventObjectTypeResolverBuilder(injectionPointTypeHierarchy.getResolver().getResolvedTypeVariables(),
                    new HierarchyDiscovery(canonicalEventType).getResolver().getResolvedTypeVariables()).build();
            resolvedType = objectTypeResolver.resolveType(canonicalEventType);
        }
        return resolvedType;
    }

    static class Notifier<T> {

        private final List<ObserverMethod<? super T>> observerMethods;

        private final EventMetadata eventMetadata;

        Notifier(List<ObserverMethod<? super T>> observerMethods, EventMetadata eventMetadata) {
            this.observerMethods = observerMethods;
            this.eventMetadata = eventMetadata;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        void notify(T event) {
            if (!observerMethods.isEmpty()) {
                EventContext eventContext = new EventContextImpl<>(event, eventMetadata);
                for (ObserverMethod<? super T> observerMethod : observerMethods) {
                    observerMethod.notify(eventContext);
                }
            }
        }

    }

    static class EventContextImpl<T> implements EventContext<T> {

        private final T payload;

        private final EventMetadata metadata;

        public EventContextImpl(T payload, EventMetadata metadata) {
            this.payload = payload;
            this.metadata = metadata;
        }

        @Override
        public T getEvent() {
            return payload;
        }

        @Override
        public EventMetadata getMetadata() {
            return metadata;
        }

    }

    static class EventMetadataImpl implements EventMetadata {

        private final Set<Annotation> qualifiers;

        private final Type eventType;

        public EventMetadataImpl(Set<Annotation> qualifiers, Type eventType) {
            this.qualifiers = qualifiers;
            this.eventType = eventType;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public InjectionPoint getInjectionPoint() {
            // TODO add partial support
            return null;
        }

        @Override
        public Type getType() {
            return eventType;
        }

    }

}
