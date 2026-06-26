package io.quarkus.hibernate.reactive.runtime.transaction;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.PERSISTENCE_UNIT_NAME_KEY;

import java.util.Optional;

import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.reactive.transaction.runtime.ReactiveTransactionSynchronization;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

/**
 * Reactive transaction synchronization for Hibernate Reactive,
 * analogous to Hibernate ORM's JTA synchronizations.
 * <p>
 * Registered programmatically when a session is first opened within a {@code @Transactional} method.
 */
public class HibernateReactiveSynchronization implements ReactiveTransactionSynchronization {

    private static Optional<String> getPersistenceUnitName(Context context) {
        return ContextLocals.get(PERSISTENCE_UNIT_NAME_KEY);
    }

    /**
     * Flush all opened sessions. This must be called before commit/rollback
     * so that dirty state is written to the DB within the open transaction.
     */
    @Override
    public Uni<?> beforeCommit(Context context) {
        Optional<String> optPersistenceUnitName = getPersistenceUnitName(context);
        return optPersistenceUnitName.map(persistenceUnitName -> Uni.combine().all().unis(
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE.flushSession(context, persistenceUnitName),
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATELESS.flushSession(context, persistenceUnitName))
                .discardItems()).orElse(Uni.createFrom().voidItem());
    }

    /**
     * Close all opened sessions and clean up.
     * This must be called after commit/rollback to avoid Hibernate Reactive HR000090 validation
     */
    @Override
    public Uni<?> afterCommit(Context context) {
        Optional<String> optPersistenceUnitName = getPersistenceUnitName(context);
        return optPersistenceUnitName.map(persistenceUnitName -> Uni.combine().all().unis(
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE.closeSession(context, persistenceUnitName),
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATELESS.closeSession(context, persistenceUnitName))
                .discardItems()).orElse(Uni.createFrom().voidItem())
                .eventually(() -> {
                    // We want to make sure that we clear the state after the closing (and after the flushing) as well
                    ContextLocals.remove(PERSISTENCE_UNIT_NAME_KEY);
                });
    }
}
