package io.quarkus.reactive.transaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
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
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Transaction;

/**
 * The base interceptor which manages reactive transactions for methods
 * annotated with {@link Transactional}.
 * Each value has its own class as the QuarkusReactiveTransaction Type is binding so requires exact match
 */
public abstract class TransactionalInterceptorBase {

    // Used in this class and in TransactionalContextPool to store and get
    // the lazily created Transaction inside the Vert.x Context
    public static final String CURRENT_TRANSACTION_KEY = "reactive.transaction.currentTransaction";

    // This key is used to indicate the method was annotated with @Transactional
    // And will open a session and a transaction lazily when the first operation requires a reactive session
    // Check HibernateReactiveRecorder.sessionSupplier to see where the session is injected
    public static final String TRANSACTIONAL_METHOD_KEY = "hibernate.reactive.methodTransactional";

    // This key is used by Panache internally it's the marker key the WithSessionOnDemand interceptor uses
    public static final String SESSION_ON_DEMAND_KEY = "hibernate.reactive.panache.sessionOnDemand";

    // This key is used by Panache internally it's the marker key the WithTransaction interceptor uses
    public static final String WITH_TRANSACTION_METHOD_KEY = "hibernate.reactive.withTransaction";

    private static final Logger LOG = Logger.getLogger(TransactionalInterceptorBase.class);

    private static final String ERROR_MSG = "@Transactional reactive support requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private AfterWorkStrategy<?> afterWorkStrategy;

    public Object doIntercept(InvocationContext context, AfterWorkStrategy<?> afterWorkStrategy) throws Exception {
        Method method = context.getMethod();
        LOG.tracef("Starting Transactional interceptor from method %s", method);
        this.afterWorkStrategy = afterWorkStrategy;
        if (isUniReturnType(context)) {
            Optional<Uni<Object>> typeValidation = validateTransactionalType(context);

            if (typeValidation.isPresent()) {
                return typeValidation.get();
            }

            Transactional annotation = getTransactional(context);
            return withTransactionalSessionOnDemand(annotation, method, () -> {
                return proceedUni(context);
            });
        }
        LOG.tracef("Transactional interceptor end from method %s", method);
        return context.proceed();
    }

    Transaction transaction() {
        return Vertx.currentContext().getLocal(CURRENT_TRANSACTION_KEY);
    }

    // Based on org/hibernate/reactive/pool/impl/SqlClientConnection.java:305
    Uni<Void> commit() {
        Transaction transaction = transaction();
        if (transaction == null) {
            // This might happen if the method is annotated with @Transactional but doesn't flush
            // i.e. a single persist without an explicit .flush()
            // We then avoid committing the transaction here, and we rely on Hibernate Reactive
            // committing the transaction after closing
            LOG.tracef("Transaction doesn't exist, so won't commit here %s");
            return Uni.createFrom().nullItem();
        }

        return Uni.createFrom().completionStage(transaction.commit()
                .onSuccess(v -> LOG.tracef("Transaction committed: %s", transaction))
                .onFailure(v -> LOG.tracef("Failed to commit transaction: %s", transaction))
                .toCompletionStage());
    }

    Uni<Void> rollbackOnCancel() {
        Transaction transaction = transaction();
        return Uni.createFrom().completionStage(transaction.rollback()
                .onFailure(v -> {
                    LOG.tracef("Failed to rollback transaction on cancellation: %s", transaction);
                })
                .onSuccess(ignored -> {
                    LOG.tracef("Transaction rolled back due to cancellation: %s", transaction);
                })
                .toCompletionStage());
    }

    // Based on org/hibernate/reactive/pool/impl/SqlClientConnection.java:314
    Uni<Void> rollbackOrCommitBasedOnException(Transactional annotation, Throwable exception) {
        Transaction transaction = transaction();

        for (Class<?> dontRollbackOnClass : annotation.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(exception.getClass())) {
                LOG.trace("Avoid rollback due to `dontRollbackOn` on `@Transactional` annotation, committing instead");
                return commit();
            }
        }

        for (Class<?> rollbackOnClass : annotation.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(exception.getClass())) {
                LOG.tracef(
                        "Rollback the transaction due to exception class %s included in `rollbackOn` field on `@Transactional` annotation",
                        exception.getClass());
                return actualRollback(transaction);
            }
        }

        Rollback rollbackAnnotation = exception.getClass().getAnnotation(Rollback.class);
        if (rollbackAnnotation != null) {
            if (rollbackAnnotation.value()) {
                LOG.tracef("Rollback the transaction as the exception class %s is annotated with `@Rollback` annotation",
                        exception.getClass());
                return actualRollback(transaction);
            } else {
                LOG.tracef(
                        "Do not rollback the transaction as the exception class %s is annotated with `@Rollback(false)` annotation",
                        exception.getClass());
                return commit();
            }
        }

        // Default behavior: rollback for RuntimeException and Error (unchecked exceptions)
        // Note: Mutiny wraps checked exceptions in CompletionException, so they appear as RuntimeException here
        return actualRollback(transaction);
    }

    private Uni<Void> actualRollback(Transaction transaction) {
        return Uni.createFrom().completionStage(
                transaction.rollback()
                        .onFailure(v -> LOG.tracef("Failed to rollback transaction: %s", transaction))
                        .onSuccess(ignored -> LOG.tracef("Transaction rolled back: %s", transaction))
                        .toCompletionStage());
    }

    @SuppressWarnings("unchecked")
    public static <T> Uni<T> proceedUni(InvocationContext context) {
        try {
            return ((Uni<T>) context.proceed());
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
    }

    // TODO copied from Panache -- refactor and put in a common module?
    public static boolean isUniReturnType(InvocationContext context) {
        return context.getMethod().getReturnType().equals(Uni.class);
    }

    protected <T> Uni<T> withTransactionalSessionOnDemand(Transactional annotation, Method method, Supplier<Uni<T>> work) {
        Context context = vertxContext();
        if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
            return Uni.createFrom().failure(
                    new UnsupportedOperationException(
                            "Cannot call a method annotated with @Transactional from a method annotated with @WithSessionOnDemand"));
        }

        if (context.getLocal(WITH_TRANSACTION_METHOD_KEY) != null) {
            return Uni.createFrom().failure(
                    new UnsupportedOperationException(
                            "Cannot call a method annotated with @Transactional from a method annotated with @WithTransaction"));
        }

        // TODO check that there's no other session opened by session delegators for another PU

        // TODO check that there's no statelessSession opened by statelessSession delegators

        // io/quarkus/hibernate/reactive/panache/common/runtime/SessionOperations.java:79
        if (context.getLocal(TRANSACTIONAL_METHOD_KEY) != null) {
            return work.get();
        } else {
            // mark this method to be @Transactional so that other Panache interceptor might fail
            LOG.tracef("Setting this method as transactional: %s", method);
            context.putLocal(TRANSACTIONAL_METHOD_KEY, true);
            // perform the work and eventually close the session and remove the key
            return work.get().eventually(() -> {

                return Uni.combine().all().unis(afterWorkStrategy.getAfterWorkActions(context)).discardItems();

            }).onFailure()
                    .call(exception -> {
                        return rollbackOrCommitBasedOnException(annotation, exception);
                    })
                    .onCancellation().call(() -> {
                        return rollbackOnCancel();
                    })
                    .call(() -> {
                        LOG.tracef("Calling commit from method %s", method);
                        return commit();
                    });
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

    protected Optional<Uni<Object>> validateTransactionalType(InvocationContext context) {
        Transactional transactional = context.getMethod().getAnnotation(Transactional.class);
        if (transactional != null && transactional.value() != Transactional.TxType.REQUIRED) {
            return Optional.of(Uni.createFrom().failure(new UnsupportedOperationException(
                    "@Transactional on Reactive methods supports only Transactional.TxType.REQUIRED")));
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Looking for the {@link Transactional} annotation first on the method,
     * second on the class.
     * <p>
     * Method handles CDI types to cover cases where extensions are used. In
     * case of EE container uses reflection.
     *
     * @param ic invocation context of the interceptor
     * @return instance of {@link Transactional} annotation or null
     */
    private Transactional getTransactional(InvocationContext ic) {
        Set<Annotation> bindings = InterceptorBindings.getInterceptorBindings(ic);
        for (Annotation i : bindings) {
            if (i.annotationType() == Transactional.class) {
                return (Transactional) i;
            }
        }
        throw new RuntimeException("Cannot find a @Transactional annotation");
    }
}
