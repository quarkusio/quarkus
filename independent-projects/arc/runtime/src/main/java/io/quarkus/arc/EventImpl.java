/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.event.ObserverException;
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

    private static final Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();

    private static final NotificationOptions DEFAULT_OPTIONS = NotificationOptions.ofExecutor(DEFAULT_EXECUTOR);

    private final HierarchyDiscovery injectionPointTypeHierarchy;

    private final Type eventType;

    private final Set<Annotation> qualifiers;

    private final ConcurrentMap<Class<?>, Notifier<? super T>> notifiers;

    private transient volatile Notifier<? super T> lastNotifier;

    public EventImpl(Type eventType, Set<Annotation> qualifiers) {
        if (eventType instanceof ParameterizedType) {
            eventType = ((ParameterizedType) eventType).getActualTypeArguments()[0];
        }
        this.eventType = eventType;
        this.injectionPointTypeHierarchy = new HierarchyDiscovery(eventType);
        this.qualifiers = qualifiers;
        this.qualifiers.add(Any.Literal.INSTANCE);
        this.notifiers = new ConcurrentHashMap<>(DEFAULT_CACHE_CAPACITY);
    }

    @Override
    public void fire(T event) {
        getNotifier(event.getClass()).notify(event, ObserverExceptionHandler.IMMEDIATE_HANDLER, false);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        return fireAsync(event, DEFAULT_OPTIONS);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        Objects.requireNonNull(options);

        @SuppressWarnings("unchecked")
        Notifier<U> notifier = (Notifier<U>) getNotifier(event.getClass());

        Executor executor = options.getExecutor();
        if (executor == null) {
            executor = DEFAULT_EXECUTOR;
        }

        if (notifier.isEmpty()) {
            return AsyncEventDeliveryStage.completed(event, executor);
        }

        Supplier<U> notifyLogic = () -> {
            ObserverExceptionHandler exceptionHandler = new CollectingExceptionHandler();
            notifier.notify(event, exceptionHandler, true);
            handleExceptions(exceptionHandler);
            return event;
        };

        Supplier<U> withinRequest = () -> {
            ArcContainer container = Arc.container();
            if (container.getActiveContext(RequestScoped.class) != null) {
                return notifyLogic.get();
            } else {
                ManagedContext requestContext = container.requestContext();
                try {
                    requestContext.activate();
                    return notifyLogic.get();
                } finally {
                    requestContext.terminate();
                }
            }
        };
        CompletableFuture<U> completableFuture = CompletableFuture.supplyAsync(withinRequest, executor);
        return new AsyncEventDeliveryStage<>(completableFuture, executor);
    }

    private Notifier<? super T> getNotifier(Class<?> runtimeType) {
        Notifier<? super T> notifier = this.lastNotifier;
        if (notifier != null && notifier.runtimeType.equals(runtimeType)) {
            return notifier;
        }
        return this.lastNotifier = notifiers.computeIfAbsent(runtimeType, this::createNotifier);
    }

    @Override
    public Event<T> select(Annotation... qualifiers) {
        Set<Annotation> mergerdQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergerdQualifiers, qualifiers);
        return new EventImpl<T>(eventType, mergerdQualifiers);
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        Set<Annotation> mergerdQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergerdQualifiers, qualifiers);
        return new EventImpl<U>(subtype, mergerdQualifiers);
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        Set<Annotation> mergerdQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergerdQualifiers, qualifiers);
        return new EventImpl<U>(subtype.getType(), mergerdQualifiers);
    }

    private Notifier<? super T> createNotifier(Class<?> runtimeType) {
        Type eventType = getEventType(runtimeType);
        return createNotifier(runtimeType, eventType, qualifiers, ArcContainerImpl.unwrap(Arc.container()));
    }

    static <T> Notifier<T> createNotifier(Class<?> runtimeType, Type eventType, Set<Annotation> qualifiers,
            ArcContainerImpl container) {
        EventMetadata metadata = new EventMetadataImpl(qualifiers, eventType);
        List<ObserverMethod<? super T>> notifierObserverMethods = new ArrayList<>();
        for (ObserverMethod<? super T> observerMethod : container.resolveObservers(eventType, qualifiers)) {
            notifierObserverMethods.add(observerMethod);
        }
        return new Notifier<>(runtimeType, notifierObserverMethods, metadata);
    }

    private Type getEventType(Class<?> runtimeType) {
        Type resolvedType = runtimeType;
        if (Types.containsTypeVariable(resolvedType)) {
            /*
             * If the container is unable to resolve the parameterized type of the event object, it uses the specified type to
             * infer the parameterized type of
             * the event types.
             */
            resolvedType = injectionPointTypeHierarchy.resolveType(resolvedType);
        }
        if (Types.containsTypeVariable(resolvedType)) {
            /*
             * Examining the hierarchy of the specified type did not help. This may still be one of the cases when combining the
             * event type and the specified
             * type reveals the actual values for type variables. Let's try that.
             */
            Type canonicalEventType = Types.getCanonicalType(runtimeType);
            TypeResolver objectTypeResolver = new EventObjectTypeResolverBuilder(
                    injectionPointTypeHierarchy.getResolver().getResolvedTypeVariables(),
                    new HierarchyDiscovery(canonicalEventType).getResolver().getResolvedTypeVariables()).build();
            resolvedType = objectTypeResolver.resolveType(canonicalEventType);
        }
        return resolvedType;
    }

    private void handleExceptions(ObserverExceptionHandler handler) {
        List<Throwable> handledExceptions = handler.getHandledExceptions();
        if (!handledExceptions.isEmpty()) {
            CompletionException exception = null;
            if (handledExceptions.size() == 1) {
                exception = new CompletionException(handledExceptions.get(0));
            } else {
                exception = new CompletionException(null);
            }
            for (Throwable handledException : handledExceptions) {
                exception.addSuppressed(handledException);
            }
            throw exception;
        }
    }

    static class Notifier<T> {

        private final Class<?> runtimeType;

        private final List<ObserverMethod<? super T>> observerMethods;

        private final EventMetadata eventMetadata;

        Notifier(Class<?> runtimeType, List<ObserverMethod<? super T>> observerMethods, EventMetadata eventMetadata) {
            this.runtimeType = runtimeType;
            this.observerMethods = observerMethods;
            this.eventMetadata = eventMetadata;
        }

        void notify(T event) {
            notify(event, ObserverExceptionHandler.IMMEDIATE_HANDLER, false);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        void notify(T event, ObserverExceptionHandler exceptionHandler, boolean async) {
            if (!isEmpty()) {
                EventContext eventContext = new EventContextImpl<>(event, eventMetadata);
                for (ObserverMethod<? super T> observerMethod : observerMethods) {
                    if (observerMethod.isAsync() == async) {
                        try {
                            observerMethod.notify(eventContext);
                        } catch (Throwable e) {
                            exceptionHandler.handle(e);
                        }
                    }
                }
            }
        }

        boolean isEmpty() {
            return observerMethods.isEmpty();
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

    /**
     * There are two different strategies of exception handling for observer methods. When an exception is raised by a
     * synchronous or transactional observer for
     * a synchronous event, this exception stops the notification chain and the exception is propagated immediately. On the
     * other hand, an exception thrown
     * during asynchronous event delivery never is never propagated directly. Instead, all the exceptions for a given
     * asynchronous event are collected and then
     * made available together using CompletionException.
     *
     * @author Jozef Hartinger
     *
     */
    protected interface ObserverExceptionHandler {

        ObserverExceptionHandler IMMEDIATE_HANDLER = throwable -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            throw new ObserverException(throwable);
        };

        void handle(Throwable throwable);

        default List<Throwable> getHandledExceptions() {
            return Collections.emptyList();
        }
    }

    static class CollectingExceptionHandler implements ObserverExceptionHandler {

        private List<Throwable> throwables;

        CollectingExceptionHandler() {
            this(new LinkedList<>());
        }

        CollectingExceptionHandler(List<Throwable> throwables) {
            this.throwables = throwables;
        }

        @Override
        public void handle(Throwable throwable) {
            throwables.add(throwable);
        }

        @Override
        public List<Throwable> getHandledExceptions() {
            return throwables;
        }
    }

}
