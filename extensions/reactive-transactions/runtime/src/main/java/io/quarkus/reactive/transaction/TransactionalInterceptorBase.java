package io.quarkus.reactive.transaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.transaction.annotations.Rollback;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

/**
 * The base interceptor which manages reactive transactions for methods
 * annotated with {@link Transactional}.
 * Each Transaction Type (REQUIRED, MANDATORY, NEVER, etc...)
 * has its own class as the QuarkusReactiveTransaction is a binding type so requires exact match
 */
public abstract class TransactionalInterceptorBase {

    // Used in this class and in TransactionalContextPool to store and get
    // the connection with the lazily created Transaction inside the Vert.x Context
    public static final String CURRENT_CONNECTION_KEY = "reactive.transaction.currentConnection";

    // This key is used to indicate the method was annotated with @Transactional
    // And will open a session and a transaction lazily when the first operation requires a reactive session
    // Check HibernateReactiveRecorder.sessionSupplier to see where the session is injected
    public static final String TRANSACTIONAL_METHOD_KEY = "hibernate.reactive.methodTransactional";

    // This key is used to store the name of the session that is currently injected with @Inject
    public static final String PERSISTENCE_UNIT_NAME_KEY = "hibernate.reactive.persistenceUnitNameKey";

    // This key is used by Panache internally it's the marker key the WithSessionOnDemand interceptor uses
    public static final String SESSION_ON_DEMAND_KEY = "hibernate.reactive.panache.sessionOnDemand";

    // This key is used by Panache internally it's the marker key the WithTransaction interceptor uses
    public static final String WITH_TRANSACTION_METHOD_KEY = "hibernate.reactive.withTransaction";

    // This key is used by Panache internally it's the marker key the ReactiveTransactional interceptor uses
    public static final String REACTIVE_TRANSACTIONAL_METHOD_KEY = "hibernate.reactive.reactiveTransactional";

    private static final Logger LOG = Logger.getLogger(TransactionalInterceptorBase.class);

    private static final String ERROR_MSG = "@Transactional reactive support requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private ReactiveResource reactiveResource;

    public Object doIntercept(InvocationContext context, ReactiveResource reactiveResource) throws Exception {
        Method method = context.getMethod();
        LOG.tracef("Starting Transactional interceptor from method %s", method);
        this.reactiveResource = reactiveResource;

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
        Context context = vertxContext();

        if (context.getLocal(TRANSACTIONAL_METHOD_KEY) == null) {
            // This is the parent method, responsible for commit, rollback or cancellation of the transaction

            /*
             * Mark the current context to be @Transactional
             * This is the parent method and will handle commit / rollback
             * Subsequent nested methods will avoid tx management
             */
            LOG.tracef("Setting this method as transactional: %s", method);
            context.putLocal(TRANSACTIONAL_METHOD_KEY, true);

            return work.get()
                    .onFailure().call(exception -> {
                        return rollbackOrCommitBasedOnException(context, annotation, exception);
                    })
                    .onCancellation().call(() -> {
                        return rollbackOnCancel();
                    })
                    .call(() -> { // Good path - commit
                        LOG.tracef("Calling commit from method %s", method);
                        return invokeBeforeCommitAndCommit(context);
                    })
                    .eventually(() -> {
                        return reactiveResource.afterCommit(context);
                    }).eventually(() -> {
                        return closeConnection();
                    });
        } else {
            // Nested methods should just propagate the reactive chain without transaction handling
            return work.get();
        }
    }

    private Uni<Void> invokeBeforeCommitAndCommit(Context context) {
        return reactiveResource.beforeCommit(context)
                .onItem().invoke(() -> LOG.tracef("Flushed the session before commit/rollback"))
                .onItemOrFailure().call((result, exception) -> {
                    if (exception != null) {
                        SqlConnection connection = connectionFromContext();
                        return actualRollback(connection.transaction(), exception).invoke(() -> {
                            // onItemOrFailure() will still propagate the chain even with an execption
                            // we need to rethrow it to make sure the reactive chain fails
                            throw new RuntimeException("Transaction rolled back due to: ", exception);
                        });
                    } else {
                        return commit();
                    }
                }).replaceWithVoid();
    }

    private Uni<?> closeConnection() {
        SqlConnection connection = connectionFromContext();
        if (connection == null) {
            // io/quarkus/hibernate/reactive/transaction/DisableJTATransactionTest.java:38
            LOG.tracef("Connection doesn't exist, nothing to do here");
            return Uni.createFrom().nullItem();
        }
        LOG.tracef("Closing the connection %s", connection);
        return toUni(connection.close());
    }

    SqlConnection connectionFromContext() {
        return Vertx.currentContext().getLocal(CURRENT_CONNECTION_KEY);
    }

    // Based on org/hibernate/reactive/pool/impl/SqlClientConnection.java:305
    Uni<Void> commit() {
        SqlConnection connection = connectionFromContext();
        if (connection == null || connection.transaction() == null) {
            // This might happen if the method is annotated with @Transactional but doesn't flush
            // i.e. a single persist without an explicit .flush()
            // We then avoid committing the transaction here, and we rely on Hibernate Reactive
            // committing the transaction after closing
            LOG.tracef("Transaction doesn't exist, so won't commit here");
            return Uni.createFrom().nullItem();
        }

        return toUni(connection.transaction().commit())
                .onFailure().invoke(() -> LOG.tracef("Failed to commit transaction: %s", connection))
                .invoke(() -> LOG.tracef("Transaction committed: %s", connection));
    }

    private static <T> Uni<T> toUni(Future<T> future) {
        return Uni.createFrom()
                .emitter(emitter -> future.onComplete(emitter::complete, emitter::fail));
    }

    Uni<Void> rollbackOnCancel() {
        SqlConnection connection = connectionFromContext();
        Transaction transaction = connection.transaction();
        return toUni(transaction.rollback())
                .onFailure().invoke(() -> LOG.tracef("Failed to rollback transaction on cancellation: %s", connection))
                .invoke(() -> LOG.tracef("Transaction rolled back due to cancellation: %s", transaction));
    }

    // Based on org/hibernate/reactive/pool/impl/SqlClientConnection.java:314
    Uni<Void> rollbackOrCommitBasedOnException(Context context, Transactional annotation, Throwable exception) {
        SqlConnection connection = connectionFromContext();

        for (Class<?> dontRollbackOnClass : annotation.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(exception.getClass())) {
                LOG.trace("Avoid rollback due to `dontRollbackOn` on `@Transactional` annotation, committing instead");
                return invokeBeforeCommitAndCommit(context);
            }
        }

        for (Class<?> rollbackOnClass : annotation.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(exception.getClass())) {
                LOG.tracef(
                        "Rollback the transaction due to exception class %s included in `rollbackOn` field on `@Transactional` annotation",
                        exception.getClass());
                return actualRollback(connection.transaction(), exception);
            }
        }

        Rollback rollbackAnnotation = exception.getClass().getAnnotation(Rollback.class);
        if (rollbackAnnotation != null) {
            if (rollbackAnnotation.value()) {
                LOG.tracef("Rollback the transaction as the exception class %s is annotated with `@Rollback` annotation",
                        exception.getClass());
                return actualRollback(connection.transaction(), exception);
            } else {
                LOG.tracef(
                        "Do not rollback the transaction as the exception class %s is annotated with `@Rollback(false)` annotation",
                        exception.getClass());
                return invokeBeforeCommitAndCommit(context);
            }
        }

        // Default behavior: rollback for RuntimeException and Error (unchecked exceptions)
        // Note: Mutiny wraps checked exceptions in CompletionException, so they appear as RuntimeException here
        return actualRollback(connection.transaction(), exception);
    }

    private Uni<Void> actualRollback(Transaction transaction, Throwable exception) {
        return toUni(transaction.rollback())
                .onFailure().invoke(() -> LOG.tracef("Failed to rollback transaction: %s", transaction))
                .invoke(() -> LOG.tracef("Transaction rolled back: %s due to exception %s", transaction, exception));
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
        Context context = vertxContext();
        if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
            throw new UnsupportedOperationException(
                    "Calling a method annotated with @Transactional from a method annotated with @WithSessionOnDemand is not supported. "
                            + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                            + "but not both, throughout your whole application.");
        }

        if (context.getLocal(WITH_TRANSACTION_METHOD_KEY) != null) {
            throw new UnsupportedOperationException(
                    "Calling a method annotated with @Transactional from a method annotated with @WithTransaction is not supported. "
                            + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                            + "but not both, throughout your whole application.");
        }

        if (context.getLocal(REACTIVE_TRANSACTIONAL_METHOD_KEY) != null) {
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
