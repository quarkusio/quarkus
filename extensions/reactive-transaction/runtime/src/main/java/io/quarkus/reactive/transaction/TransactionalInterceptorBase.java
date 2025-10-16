package io.quarkus.reactive.transaction;

import java.lang.annotation.Annotation;
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
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Transaction;

/**
 * The base intereceptor which manages reactive transactions for methods
 * annotated with {@link Transactional}.
 * Each value has its own class as the QuarkusReactiveTransaction Type is binding so requires exact match
 */
public abstract class TransactionalInterceptorBase {

    // Used in this class and in TransactionalContextPool to store and get the lazily created Transaction
    public static final String CURRENT_TRANSACTION_KEY = "reactive.transaction.currentTransaction";

    private static final Logger LOG = Logger.getLogger(TransactionalInterceptorBase.class);

    private static final String ERROR_MSG = "@Transactional reactive support requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private AfterWorkStrategy<?> afterWorkStrategy;

    public Object doIntercept(InvocationContext context, AfterWorkStrategy<?> afterWorkStrategy) throws Exception {
        this.afterWorkStrategy = afterWorkStrategy;
        if (isUniReturnType(context)) {
            Optional<Uni<Object>> typeValidation = validateTransactionalType(context);

            if (typeValidation.isPresent()) {
                return typeValidation.get();
            }

            Transactional annotation = getTransactional(context);
            return withTransactionalSessionOnDemand(() -> {
                // Handle checked exception vs runtime exception differently according to the spec
                // check blicking interceptor for java.lang.Error as well
                // copy the logic from io/quarkus/narayana/jta/runtime/interceptor/TransactionalInterceptorBase.java:363
                return proceedUni(context);
            }).onFailure()
                    .call(exception -> {
                        return rollback(annotation, Optional.of(exception));
                    })
                    .onCancellation().call(() -> {
                        return rollbackOnCancel();
                    })
                    .call(() -> {
                        return commit();
                    });
        }
        return context.proceed();
    }

    Transaction transaction() {
        return Vertx.currentContext().getLocal(CURRENT_TRANSACTION_KEY);
    }

    // Copied from org/hibernate/reactive/pool/impl/SqlClientConnection.java:305
    Uni<Void> commit() {
        Transaction transaction = transaction();
        return Uni.createFrom().completionStage(transaction.commit()
                .onSuccess(v -> LOG.tracef("Transaction committed: %s", transaction))
                .onFailure(v -> LOG.tracef("Failed to commit transaction: %s", transaction))
                .toCompletionStage());
    }

    Uni<Object> rollbackOnCancel() {
        Transaction transaction = transaction();
        return Uni.createFrom().completionStage(transaction.rollback()
                .onFailure(v -> {
                    LOG.tracef("Failed to rollback transaction on cancellation: %s", transaction);
                })
                .onSuccess(didRollback -> {
                    LOG.tracef("Transaction rolled back due to cancellation: %s", transaction);
                })
                .mapEmpty().toCompletionStage());
    }

    // Copied from org/hibernate/reactive/pool/impl/SqlClientConnection.java:314
    Uni<Object> rollback(Transactional annotation, Optional<Throwable> exception) {
        Transaction transaction = transaction();

        Future<Boolean> rollBackFuture;
        if (exception.isPresent()) {
            rollBackFuture = rollbackOnlyInSpecificCases(exception.get(), transaction, annotation);
        } else {
            rollBackFuture = transaction.rollback().map(v -> true);
        }

        return Uni.createFrom().completionStage(
                rollBackFuture
                        .onFailure(v -> LOG.tracef("Failed to rollback transaction: %s", transaction))
                        .onSuccess(didRollback -> {
                            if (didRollback) {
                                LOG.tracef("Transaction rolled back: %s", transaction);
                            } else {
                                LOG.tracef("Transaction was not rolled back: %s", transaction);
                                transaction.commit();
                                LOG.tracef("Transaction was committed: %s", transaction);
                            }
                        })
                        .mapEmpty()
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

    // This key is used to indicate the method was annotated with @Transactional
    // And will open a session and a transaction lazy when the first operation requrires a reactive session
    // Check HibernateReactiveRecorder.sessionSupplier to see where the session is injected
    // TODO Luca find a way to remove the duplication between this field and TransactionalInterceptor field
    private static final String TRANSACTIONAL_METHOD_KEY = "hibernate.reactive.methodTransactional";

    // This key is used by Panache internally it's the marker key the WithSessionOnDemand intereceptor uses
    public static final String SESSION_ON_DEMAND_KEY = "hibernate.reactive.panache.sessionOnDemand";

    // This key is used by Panache internally it's the marker key the WithTransaction intereceptor uses
    public static final String WITH_TRANSACTION_METHOD_KEY = "hibernate.reactive.withTransaction";

    protected <T> Uni<T> withTransactionalSessionOnDemand(Supplier<Uni<T>> work) {
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
            context.putLocal(TRANSACTIONAL_METHOD_KEY, true);
            // perform the work and eventually close the session and remove the key
            return work.get().eventually(() -> {
                context.removeLocal(TRANSACTIONAL_METHOD_KEY);

                // TODO Luca close session after commit
                return Uni.combine().all().unis(afterWorkStrategy.getAfterWorkActions(context)).discardItems();

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

    // Copied from io/quarkus/narayana/jta/runtime/interceptor/TransactionalInterceptorBase.java:363
    // Returns true if a rollback has been executed, false otherwise
    protected Future<Boolean> rollbackOnlyInSpecificCases(Throwable t, Transaction tx, Transactional transactional) {

        for (Class<?> dontRollbackOnClass : transactional.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(t.getClass())) {
                return Future.succeededFuture(false);
            }
        }

        for (Class<?> rollbackOnClass : transactional.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(t.getClass())) {
                return tx.rollback().map(v -> true);
            }
        }

        Rollback rollbackAnnotation = t.getClass().getAnnotation(Rollback.class);
        if (rollbackAnnotation != null) {
            if (rollbackAnnotation.value()) {
                return tx.rollback().map(v -> true);
            }
            // in both cases, behaviour is specified by the annotation
            return Future.succeededFuture(false);
        }

        // RuntimeException and Error are un-checked exceptions and rollback is expected
        if (t instanceof RuntimeException || t instanceof Error) {
            return tx.rollback().map(v -> true);
        }

        // TODO Luca No way to test that checked exception won't rollback
        // See testCheckedExceptionNoRollback
        return Future.succeededFuture(false);
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
