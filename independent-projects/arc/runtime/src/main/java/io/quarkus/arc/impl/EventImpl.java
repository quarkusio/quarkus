package io.quarkus.arc.impl;

import static io.quarkus.arc.impl.TypeCachePollutionUtils.asParameterizedType;
import static io.quarkus.arc.impl.TypeCachePollutionUtils.isParameterizedType;
import static jakarta.transaction.Status.STATUS_COMMITTED;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.event.ObserverException;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.AsyncObserverExceptionHandler;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
class EventImpl<T> implements Event<T> {

    private static final int DEFAULT_CACHE_CAPACITY = 4;
    private static final NotificationOptions EMPTY_OPTIONS = NotificationOptions.builder().build();

    private final HierarchyDiscovery injectionPointTypeHierarchy;
    private final Type eventType;
    private final Set<Annotation> qualifiers;
    private final ConcurrentMap<Class<?>, Notifier<? super T>> notifiers;
    private final InjectionPoint injectionPoint;

    private transient volatile Notifier<? super T> lastNotifier;

    private static final Logger LOGGER = Logger.getLogger(EventImpl.class);

    EventImpl(Type eventType, Set<Annotation> qualifiers, InjectionPoint injectionPoint) {
        this.eventType = initEventType(eventType);
        this.injectionPointTypeHierarchy = new HierarchyDiscovery(this.eventType);
        Set<Annotation> eventQualifiers = new HashSet<>();
        eventQualifiers.addAll(qualifiers);
        eventQualifiers.add(Any.Literal.INSTANCE);
        this.qualifiers = Set.copyOf(eventQualifiers);
        this.notifiers = new ConcurrentHashMap<>(DEFAULT_CACHE_CAPACITY);
        this.injectionPoint = injectionPoint;
    }

    @Override
    public void fire(T event) {
        Objects.requireNonNull(event, "Event cannot be null");
        getNotifier(event.getClass()).notify(event, ObserverExceptionHandler.IMMEDIATE_HANDLER, false);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        return fireAsync(event, EMPTY_OPTIONS);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        Objects.requireNonNull(options);

        @SuppressWarnings("unchecked")
        Notifier<U> notifier = (Notifier<U>) getNotifier(event.getClass());

        Executor executor = options.getExecutor();
        if (executor == null) {
            executor = Arc.container().getExecutorService();
        }

        if (notifier.isEmpty()) {
            return AsyncEventDeliveryStage.completed(event, executor);
        }

        Supplier<U> notifyLogic = new Supplier<U>() {
            @Override
            public U get() {
                // Note that async observers are notified serially - no need to synchronize the collection
                ObserverExceptionHandler exceptionHandler = new CollectingExceptionHandler(new ArrayList<>(),
                        Arc.container().instance(AsyncObserverExceptionHandler.class).get());
                notifier.notify(event, exceptionHandler, true);
                handleExceptions(exceptionHandler);
                return event;
            }
        };

        CompletableFuture<U> completableFuture = CompletableFuture.supplyAsync(notifyLogic, executor);
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
        ArcContainerImpl.instance().registeredQualifiers.verify(qualifiers);
        Set<Annotation> mergedQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergedQualifiers, qualifiers);
        return new EventImpl<T>(eventType, mergedQualifiers, injectionPoint);
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        if (Types.containsTypeVariable(subtype)) {
            throw new IllegalArgumentException(
                    "Event#select(Class<U>, Annotation...) cannot be used with type variable parameter");
        }
        ArcContainerImpl.instance().registeredQualifiers.verify(qualifiers);
        Set<Annotation> mergerdQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergerdQualifiers, qualifiers);
        return new EventImpl<U>(subtype, mergerdQualifiers, injectionPoint);
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        ArcContainerImpl.instance().registeredQualifiers.verify(qualifiers);
        if (Types.containsTypeVariable(subtype.getType())) {
            throw new IllegalArgumentException(
                    "Event#select(TypeLiteral, Annotation...) cannot be used with type variable parameter");
        }
        Set<Annotation> mergerdQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergerdQualifiers, qualifiers);
        return new EventImpl<U>(subtype.getType(), mergerdQualifiers, injectionPoint);
    }

    private Notifier<? super T> createNotifier(Class<?> runtimeType) {
        Type eventType = getEventType(runtimeType);
        return createNotifier(runtimeType, eventType, qualifiers, ArcContainerImpl.unwrap(Arc.container()), injectionPoint);
    }

    static <T> Notifier<T> createNotifier(Class<?> runtimeType, Type eventType, Set<Annotation> qualifiers,
            ArcContainerImpl container, InjectionPoint injectionPoint) {
        return createNotifier(runtimeType, eventType, qualifiers, container, !Arc.container().strictCompatibility(),
                injectionPoint);
    }

    static <T> Notifier<T> createNotifier(Class<?> runtimeType, Type eventType, Set<Annotation> qualifiers,
            ArcContainerImpl container, boolean activateRequestContext, InjectionPoint injectionPoint) {
        EventMetadata metadata = new EventMetadataImpl(qualifiers, eventType, injectionPoint);
        List<ObserverMethod<? super T>> notifierObserverMethods = new ArrayList<>(
                container.resolveObservers(eventType, qualifiers));
        return new Notifier<>(runtimeType, notifierObserverMethods, metadata, activateRequestContext);
    }

    private Type initEventType(Type type) {
        if (isParameterizedType(type)) {
            ParameterizedType parameterizedType = asParameterizedType(type);
            if (Event.class.isAssignableFrom(Types.getRawType(parameterizedType.getRawType()))) {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return type;
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
        /*
         * If the runtime type of the event object still contains an unresolved type variable,
         * the container must throw an IllegalArgumentException.
         */
        if (Types.containsTypeVariable(resolvedType)) {
            throw new IllegalArgumentException(
                    "CDI event payload cannot contain unresolved type variable; found type: " + resolvedType);
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
            // always add exceptions into suppressed
            for (Throwable handledException : handledExceptions) {
                exception.addSuppressed(handledException);
            }
            throw exception;
        }
    }

    static class Notifier<T> {

        private final Class<?> runtimeType;
        private final List<ObserverMethod<? super T>> observerMethods;
        final EventMetadata eventMetadata;
        private final boolean hasTxObservers;
        private final boolean activateRequestContext;

        Notifier(Class<?> runtimeType, List<ObserverMethod<? super T>> observerMethods, EventMetadata eventMetadata) {
            this(runtimeType, observerMethods, eventMetadata, true);
        }

        Notifier(Class<?> runtimeType, List<ObserverMethod<? super T>> observerMethods, EventMetadata eventMetadata,
                boolean activateRequestContext) {
            this.runtimeType = runtimeType;
            this.observerMethods = observerMethods;
            this.eventMetadata = eventMetadata;
            this.hasTxObservers = observerMethods.stream().anyMatch(this::isTxObserver);
            this.activateRequestContext = activateRequestContext;
        }

        void notify(T event) {
            notify(event, ObserverExceptionHandler.IMMEDIATE_HANDLER, false);
        }

        @SuppressWarnings("rawtypes")
        void notify(T event, ObserverExceptionHandler exceptionHandler, boolean async) {
            if (!isEmpty()) {

                Predicate<ObserverMethod<? super T>> predicate = async ? ObserverMethod::isAsync
                        : Predicate.not(ObserverMethod::isAsync);

                if (!async && hasTxObservers) {
                    // Note that tx observers are never async
                    InstanceHandle<TransactionManager> transactionManagerInstance = Arc.container()
                            .instance(TransactionManager.class);

                    try {
                        if (transactionManagerInstance.isAvailable() &&
                                transactionManagerInstance.get().getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                            // we have one or more transactional OM, and TransactionManager is available
                            // we attempt to register a JTA synchronization
                            List<DeferredEventNotification<?>> deferredEvents = new ArrayList<>();
                            EventContext eventContext = new EventContextImpl<>(event, eventMetadata);

                            for (ObserverMethod<? super T> om : observerMethods) {
                                if (isTxObserver(om)) {
                                    deferredEvents.add(new DeferredEventNotification<>(om, eventContext,
                                            Status.valueOf(om.getTransactionPhase())));
                                }
                            }

                            Synchronization sync = new ArcSynchronization(deferredEvents);
                            TransactionManager txManager = transactionManagerInstance.get();
                            try {
                                // NOTE - We are using standard synchronization on purpose as that seems more
                                // fitting than interposed sync. Either way will have some use-cases that won't work.
                                // See for instance discussions on https://github.com/eclipse-ee4j/cdi/issues/467
                                txManager.getTransaction().registerSynchronization(sync);
                                // registration succeeded, notify all non-tx observers synchronously
                                predicate = predicate.and(this::isNotTxObserver);
                            } catch (Exception e) {
                                if (e.getCause() instanceof RollbackException
                                        || e.getCause() instanceof IllegalStateException
                                        || e.getCause() instanceof SystemException) {
                                    // registration failed, AFTER_SUCCESS OMs are accordingly to CDI spec left out
                                    predicate = predicate.and(this::isNotAfterSuccess);
                                }
                            }
                        }
                    } catch (SystemException e) {
                        // In theory, this can be thrown by TransactionManager#getStatus() at which point we cannot even
                        // determine if we should register some synchronization, therefore, we only log this
                        LOGGER.debugf("Failure when trying to invoke TransactionManager#getStatus(). Stacktrace: %s",
                                e.getCause() != null ? e.getCause() : e);
                    }
                }

                // Non-tx observers notifications
                // req. context is activated if not in strict mode and not for lifecycle events such as init/shutdown
                if (activateRequestContext) {
                    ManagedContext requestContext = Arc.container().requestContext();
                    if (requestContext.isActive()) {
                        notifyObservers(event, exceptionHandler, predicate);
                    } else {
                        try {
                            requestContext.activate();
                            notifyObservers(event, exceptionHandler, predicate);
                        } finally {
                            requestContext.terminate();
                        }
                    }
                } else {
                    notifyObservers(event, exceptionHandler, predicate);
                }
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private void notifyObservers(T event, ObserverExceptionHandler exceptionHandler,
                Predicate<ObserverMethod<? super T>> predicate) {
            EventContext eventContext = new EventContextImpl<>(event, eventMetadata);
            for (ObserverMethod<? super T> observerMethod : observerMethods) {
                if (predicate.test(observerMethod)) {
                    try {
                        observerMethod.notify(eventContext);
                    } catch (Throwable t) {
                        exceptionHandler.handle(t, observerMethod, eventContext);
                    }
                }
            }
        }

        boolean isEmpty() {
            return observerMethods.isEmpty();
        }

        private boolean isTxObserver(ObserverMethod<?> observer) {
            return !observer.getTransactionPhase().equals(TransactionPhase.IN_PROGRESS);
        }

        private boolean isNotAfterSuccess(ObserverMethod<?> observer) {
            return !observer.getTransactionPhase().equals(TransactionPhase.AFTER_SUCCESS);
        }

        private boolean isNotTxObserver(ObserverMethod<?> observer) {
            return !isTxObserver(observer);
        }

    }

    static class ArcSynchronization implements Synchronization {

        private List<DeferredEventNotification<?>> deferredEvents;

        ArcSynchronization(List<DeferredEventNotification<?>> deferredEvents) {
            this.deferredEvents = deferredEvents;
        }

        @Override
        public void beforeCompletion() {
            for (DeferredEventNotification<?> event : deferredEvents) {
                if (event.isBeforeCompletion()) {
                    event.run();
                }
            }
        }

        @Override
        public void afterCompletion(int i) {
            for (DeferredEventNotification<?> event : deferredEvents) {
                if (!event.isBeforeCompletion() && event.getStatus().matches(i)) {
                    event.run();
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    static class DeferredEventNotification<T> implements Runnable {

        private ObserverMethod<? super T> observerMethod;
        private boolean isBeforeCompletion;
        private EventContext eventContext;
        private Status status;

        private static final Logger LOG = Logger.getLogger(DeferredEventNotification.class);

        DeferredEventNotification(ObserverMethod<? super T> observerMethod, EventContext eventContext, Status status) {
            this.observerMethod = observerMethod;
            this.isBeforeCompletion = observerMethod.getTransactionPhase().equals(TransactionPhase.BEFORE_COMPLETION);
            this.eventContext = eventContext;
            this.status = status;
        }

        public boolean isBeforeCompletion() {
            return isBeforeCompletion;
        }

        public Status getStatus() {
            return status;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                ManagedContext reqContext = Arc.container().requestContext();
                if (reqContext.isActive()) {
                    observerMethod.notify(eventContext);
                } else {
                    try {
                        reqContext.activate();
                        observerMethod.notify(eventContext);
                    } finally {
                        reqContext.terminate();
                    }
                }
            } catch (Exception e) {
                // swallow exception and log errors for every problematic OM
                LOG.errorf(
                        "Failure occurred while notifying a transational %s for event of type %s " +
                                "\n- please enable debug logging to see the full stack trace" +
                                "\n %s",
                        observerMethod, eventContext.getMetadata().getType().getTypeName(),
                        e.getCause() != null && e.getMessage() != null
                                ? "Cause: " + e.getCause() + " Message: " + e.getMessage()
                                : "Exception caught: " + e);
                LOG.debugf(e, "Failure occurred while notifying a transational %s for event of type %s",
                        observerMethod, eventContext.getMetadata().getType().getTypeName());
            }
        }
    }

    enum Status {

        ALL {
            @Override
            public boolean matches(int status) {
                return true;
            }
        },
        SUCCESS {
            @Override
            public boolean matches(int status) {
                return status == STATUS_COMMITTED;
            }
        },
        FAILURE {
            @Override
            public boolean matches(int status) {
                return status != STATUS_COMMITTED;
            }
        };

        /**
         * Indicates whether the given status code passed in during {@link Synchronization#beforeCompletion()} or
         * {@link Synchronization#afterCompletion(int)}
         * matches this status.
         *
         * @param status the given status code
         * @return true if the status code matches
         */
        public abstract boolean matches(int status);

        public static Status valueOf(TransactionPhase transactionPhase) {
            if (transactionPhase == TransactionPhase.BEFORE_COMPLETION
                    || transactionPhase == TransactionPhase.AFTER_COMPLETION) {
                return Status.ALL;
            }
            if (transactionPhase == TransactionPhase.AFTER_SUCCESS) {
                return Status.SUCCESS;
            }
            if (transactionPhase == TransactionPhase.AFTER_FAILURE) {
                return Status.FAILURE;
            }
            throw new IllegalArgumentException("Unknown transaction phase " + transactionPhase);
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

        ObserverExceptionHandler IMMEDIATE_HANDLER = (t, m, e) -> {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new ObserverException(t);
        };

        void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext);

        default List<Throwable> getHandledExceptions() {
            return Collections.emptyList();
        }
    }

    static class CollectingExceptionHandler implements ObserverExceptionHandler {

        private final List<Throwable> throwables;

        private final AsyncObserverExceptionHandler exceptionHandler;

        CollectingExceptionHandler(List<Throwable> throwables, AsyncObserverExceptionHandler exceptionHandler) {
            this.throwables = throwables;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
            throwables.add(throwable);
            try {
                exceptionHandler.handle(throwable, observerMethod, eventContext);
            } catch (Exception e) {
                LOGGER.errorf(e, "Cannot handle exception of an async observer: %s", throwable);
            }
        }

        @Override
        public List<Throwable> getHandledExceptions() {
            return throwables;
        }
    }

}
