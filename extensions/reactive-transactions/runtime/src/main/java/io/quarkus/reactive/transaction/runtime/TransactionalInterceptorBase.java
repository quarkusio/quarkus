package io.quarkus.reactive.transaction.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.transaction.annotations.Rollback;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * The base interceptor which manages reactive transactions for methods
 * annotated with {@link Transactional}.
 * Each Transaction Type (REQUIRED, MANDATORY, NEVER, etc...)
 * has its own class as the QuarkusReactiveTransaction is a binding type so requires exact match
 */
public abstract class TransactionalInterceptorBase {

    // This key is used by Hibernate Reactive to store the persistence unit name
    public static final String PERSISTENCE_UNIT_NAME_KEY = "hibernate.reactive.persistenceUnitNameKey";

    // This key is used by Panache internally it's the marker key the WithSessionOnDemand interceptor uses
    public static final String SESSION_ON_DEMAND_KEY = "hibernate.reactive.panache.sessionOnDemand";

    // This key is used by Panache internally it's the marker key the WithTransaction interceptor uses
    public static final String WITH_TRANSACTION_METHOD_KEY = "hibernate.reactive.withTransaction";

    // This key is used by Panache internally it's the marker key the ReactiveTransactional interceptor uses
    public static final String REACTIVE_TRANSACTIONAL_METHOD_KEY = "hibernate.reactive.reactiveTransactional";

    // Keep the old key as a public constant for backwards compatibility with Hibernate Reactive code
    public static final String TRANSACTIONAL_METHOD_KEY = ReactiveTransactionManager.TRANSACTIONAL_METHOD_KEY;

    private static final Logger LOG = Logger.getLogger(TransactionalInterceptorBase.class);

    private static final String ERROR_MSG = "@Transactional reactive support requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    @Inject
    ReactiveTransactionManager txManager;

    public Object doIntercept(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        LOG.tracef("Starting Transactional interceptor from method %s", method);

        if (reactiveInterceptorShouldRun()) {
            validateTransactionalType(context); // So far only REQUIRED is supported
            validateLegacyPanacheAnnotations();

            Transactional annotation = getTransactionalAnnotation(context);
            // Deferral allows the Uni to be reused (re-subscribed-to) in different Vert.x contexts.
            return Uni.createFrom()
                    .deferred(() -> defineReactiveTransactionalChain(annotation, method, () -> proceedUni(context)));
        }
        LOG.tracef("Transactional interceptor end from method %s", method);
        return context.proceed();
    }

    protected <T> Uni<T> defineReactiveTransactionalChain(Transactional annotation, Method method, Supplier<Uni<T>> work) {
        // We are running on the retrieved context, however, the method also switch the safety flag.
        Context context = vertxContext();

        if (!txManager.isActive()) {
            // This is the parent method, responsible for commit, rollback or cancellation of the transaction
            LOG.tracef("Setting this method as transactional: %s", method);
            txManager.begin();

            return work.get()
                    .onFailure().call(exception -> rollbackOrCommitBasedOnException(annotation, exception))
                    .onCancellation().call(() -> txManager.rollback())
                    .call(() -> {
                        LOG.tracef("Calling commit from method %s", method);
                        return txManager.commit();
                    })
                    .eventually(() -> txManager.close());
        } else {
            // Nested methods should just propagate the reactive chain without transaction handling
            return work.get();
        }
    }

    Uni<Void> rollbackOrCommitBasedOnException(Transactional annotation, Throwable exception) {
        for (Class<?> dontRollbackOnClass : annotation.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(exception.getClass())) {
                LOG.trace("Avoid rollback due to `dontRollbackOn` on `@Transactional` annotation, committing instead");
                return txManager.commit();
            }
        }

        for (Class<?> rollbackOnClass : annotation.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(exception.getClass())) {
                LOG.tracef(
                        "Rollback the transaction due to exception class %s included in `rollbackOn` field on `@Transactional` annotation",
                        exception.getClass());
                return txManager.rollback();
            }
        }

        Rollback rollbackAnnotation = exception.getClass().getAnnotation(Rollback.class);
        if (rollbackAnnotation != null) {
            if (rollbackAnnotation.value()) {
                LOG.tracef(
                        "Rollback the transaction as the exception class %s is annotated with `@Rollback` annotation",
                        exception.getClass());
                return txManager.rollback();
            } else {
                LOG.tracef(
                        "Do not rollback the transaction as the exception class %s is annotated with `@Rollback(false)` annotation",
                        exception.getClass());
                return txManager.commit();
            }
        }

        // Default behavior: rollback for RuntimeException and Error (unchecked exceptions)
        // Note: Mutiny wraps checked exceptions in CompletionException, so they appear as RuntimeException here
        return txManager.rollback();
    }

    @SuppressWarnings("unchecked")
    public static Uni<Object> proceedUni(InvocationContext context) {
        try {
            Object result = context.proceed();
            if (result instanceof Uni<?> uniResult) {
                return (Uni<Object>) uniResult;
            } else {
                throw new IllegalStateException(
                        "For reactive methods running on the event loop, @Transactional can only be used if the method returns a `Uni`. Found '"
                                + result.getClass().getName() + "' instead.");
            }
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
    }

    public static boolean reactiveInterceptorShouldRun() {
        boolean condition = Context.isOnEventLoopThread();
        LOG.tracef("Transactional interceptor should run: %s", condition);
        return condition;
    }

    protected void validateLegacyPanacheAnnotations() {
        // We are running on the retrieved context, however, the method also switch the safety flag.
        Context ignored = vertxContext();
        if (ContextLocals.get(SESSION_ON_DEMAND_KEY).isPresent()) {
            throw new UnsupportedOperationException(
                    "Calling a method annotated with @Transactional from a method annotated with @WithSessionOnDemand is not supported. "
                            + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                            + "but not both, throughout your whole application.");
        }

        if (ContextLocals.get(WITH_TRANSACTION_METHOD_KEY).isPresent()) {
            throw new UnsupportedOperationException(
                    "Calling a method annotated with @Transactional from a method annotated with @WithTransaction is not supported. "
                            + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                            + "but not both, throughout your whole application.");
        }

        if (ContextLocals.get(REACTIVE_TRANSACTIONAL_METHOD_KEY).isPresent()) {
            throw new UnsupportedOperationException(
                    "Calling a method annotated with @Transactional from a method annotated with @ReactiveTransactional is not supported. "
                            + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                            + "but not both, throughout your whole application.");
        }
    }

    /**
     *
     * @return the current vertx duplicated context
     * @throws IllegalStateException If no vertx context is found or is not a safe context as mandated by the
     *         {@link VertxContextSafetyToggle}
     */
    private static Context vertxContext() {
        Context context = Vertx.currentContext();
        if (context != null) {
            VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
            return context;
        } else {
            throw new IllegalStateException("No current Vertx context found");
        }
    }

    // Default impl fails .REQUIRED overrides it
    protected void validateTransactionalType(InvocationContext context) {
        Transactional transactional = context.getMethod().getAnnotation(Transactional.class);
        if (transactional != null && transactional.value() != Transactional.TxType.REQUIRED) {
            throw new UnsupportedOperationException(
                    "@Transactional on Reactive methods supports only Transactional.TxType.REQUIRED");
        }
    }

    /**
     * <p>
     * Looking for the {@link Transactional} annotation first on the method,
     * second on the class.
     * <p>
     * Method handles CDI types to cover cases where extensions are used. In
     * case of EE container uses reflection.
     *
     * @param context invocation context of the interceptor
     * @return instance of {@link Transactional} annotation or null
     */
    private Transactional getTransactionalAnnotation(InvocationContext context) {
        Set<Annotation> bindings = InterceptorBindings.getInterceptorBindings(context);
        for (Annotation binding : bindings) {
            if (binding.annotationType() == Transactional.class) {
                return (Transactional) binding;
            }
        }
        throw new RuntimeException("Cannot find a @Transactional annotation");
    }
}
