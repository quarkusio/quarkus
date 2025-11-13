package io.quarkus.hibernate.reactive.transactions.runtime;

import static io.quarkus.hibernate.reactive.runtime.customized.TransactionalContextPool.CURRENT_TRANSACTION_KEY;

import java.util.Optional;
import java.util.function.Supplier;

import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Transaction;

/**
 * The base intereceptor which manages reactive transactions for methods
 * annotated with {@link Transactional}.
 * Each value has its own class as the QuarkusReactiveTransaction Type is binding so requires exact match
 */
public abstract class TransactionalInterceptorBase {

    private static final Logger LOG = Logger.getLogger(TransactionalInterceptorBase.class);

    private static final String ERROR_MSG = "Hibernate Reactive Panache requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    public Object intercept(InvocationContext context) throws Exception {
        if (isUniReturnType(context)) {
            Optional<Uni<Object>> typeValidation = validateTransactionalType(context);

            if (typeValidation.isPresent()) {
                return typeValidation.get();
            }

            return withTransactionalSessionOnDemand(() -> {
                // Handle checked exception vs runtime exception differently according to the spec
                // check blicking interceptor for java.lang.Error as well
                // copy the logic from io/quarkus/narayana/jta/runtime/interceptor/TransactionalInterceptorBase.java:363
                return proceedUni(context);
            }).onFailure().call(this::rollback)
                    .onCancellation().call(this::rollback)
                    .call(this::commit);
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

    // Copied from org/hibernate/reactive/pool/impl/SqlClientConnection.java:314
    Uni<Void> rollback() {
        Transaction transaction = transaction();
        return Uni.createFrom().completionStage(transaction.rollback()
                .onFailure(v -> LOG.tracef("Failed to rollback transaction: %s", transaction))
                .onSuccess(v -> LOG.tracef("Transaction rolled back: %s", transaction))
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

    static <T> Uni<T> withTransactionalSessionOnDemand(Supplier<Uni<T>> work) {
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
                return Uni.combine().all().unis(
                        HibernateReactiveRecorder.OPENED_SESSIONS_STATE.closeAllOpenedSessions(context),
                        HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATLESS.closeAllOpenedSessions(context))
                        .discardItems();
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
}
