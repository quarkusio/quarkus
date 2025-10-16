package io.quarkus.hibernate.reactive.runtime.transaction;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.PERSISTENCE_UNIT_NAME_KEY;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.reactive.transaction.ReactiveResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@ApplicationScoped
public class HibernateActionsStrategy implements ReactiveResource {

    // This can be null if a method is annotated with @Transactional but doesn't inject a session
    private static Optional<String> getPersistenceUnitName(Context context) {
        return Optional.ofNullable(context.getLocal(PERSISTENCE_UNIT_NAME_KEY));
    }

    /**
     * Flush all opened sessions. This must be called before commit/rollback
     * so that dirty state is written to the DB within the open transaction.
     */
    @Override
    public Uni<Void> beforeCommit(Context context) {
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
    public Uni<Void> afterCommit(Context context) {
        Optional<String> optPersistenceUnitName = getPersistenceUnitName(context);
        return optPersistenceUnitName.map(persistenceUnitName -> Uni.combine().all().unis(
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE.closeSession(context, persistenceUnitName),
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATELESS.closeSession(context, persistenceUnitName))
                .discardItems()).orElse(Uni.createFrom().voidItem())
                .eventually(() -> {
                    // We want to make sure that we clear the state after the closing (and after the flushing) as well
                    context.removeLocal(TRANSACTIONAL_METHOD_KEY);
                    context.removeLocal(PERSISTENCE_UNIT_NAME_KEY);
                });

    }
}
