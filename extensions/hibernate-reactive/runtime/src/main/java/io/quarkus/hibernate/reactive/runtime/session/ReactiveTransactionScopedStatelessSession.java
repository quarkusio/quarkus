package io.quarkus.hibernate.reactive.runtime.session;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.PERSISTENCE_UNIT_NAME_KEY;
import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.SESSION_ON_DEMAND_KEY;
import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.Optional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.delegation.MutinyStatelessSessionDelegator;
import org.jboss.logging.Logger;

import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class ReactiveTransactionScopedStatelessSession extends MutinyStatelessSessionDelegator {

    private static final Logger LOG = Logger.getLogger(ReactiveTransactionScopedStatelessSession.class);

    private static final OpenedSessionsState<Mutiny.StatelessSession> STATE = new OpenedSessionsStateStatelessImpl();

    private final String persistenceUnitName;

    public ReactiveTransactionScopedStatelessSession(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public Mutiny.StatelessSession delegate() {
        Context context = Vertx.currentContext();

        Optional<Mutiny.StatelessSession> existingSession = getExistingSession(context, persistenceUnitName);
        if (existingSession.isPresent()) {
            return existingSession.get();
        } else if (ContextLocals.get(TRANSACTIONAL_METHOD_KEY).isEmpty()) {
            throw new IllegalStateException(
                    "No current Mutiny.StatelessSession found"
                            + "\n\t- you need to annotate your method with @Transactional to open a reactive session"
                            + "\n\t- alternatively, you can use @WithSessionOnDemand, @WithSession, or @WithTransaction"
                            + "\n\t- for JAX-RS resources, annotate the method directly with an HTTP method (@GET, @POST, etc.) to automatically open a session");
        } else {
            return openStatelessSession(persistenceUnitName);
        }
    }

    /**
     * Returns an existing stateless session for the given persistence unit, or opens a new one.
     */
    public static Mutiny.StatelessSession getOrOpenStatelessSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();
        Optional<Mutiny.StatelessSession> existingSession = getExistingSession(context, persistenceUnitName);
        if (existingSession.isPresent()) {
            return existingSession.get();
        } else if (ContextLocals.get(TRANSACTIONAL_METHOD_KEY).isEmpty()
                && ContextLocals.get(SESSION_ON_DEMAND_KEY).isEmpty()) {
            throw new IllegalStateException(
                    "No current Mutiny.StatelessSession found"
                            + "\n\t- you need to annotate your method with @Transactional to open a reactive session"
                            + "\n\t- alternatively, you can use @WithSessionOnDemand, @WithSession, or @WithTransaction"
                            + "\n\t- for JAX-RS resources, annotate the method directly with an HTTP method (@GET, @POST, etc.) to automatically open a session");
        }
        return openStatelessSession(persistenceUnitName);
    }

    public static Optional<Mutiny.StatelessSession> getExistingSession(Context context, String persistenceUnitName) {
        return STATE.getOpenedSession(context, persistenceUnitName)
                .map(OpenedSessionsState.SessionWithKey::session);
    }

    public static Mutiny.StatelessSession createNewSession(String persistenceUnitName, Context context) {
        return STATE.createNewSession(persistenceUnitName, context);
    }

    public static Uni<Void> flushSession(Context context, String persistenceUnitName) {
        return STATE.flushSession(context, persistenceUnitName);
    }

    public static Uni<Void> closeSession(Context context, String persistenceUnitName) {
        return STATE.closeSession(context, persistenceUnitName);
    }

    public static Optional<Mutiny.Transaction> currentTransaction(Context context, String persistenceUnitName) {
        return STATE.currentTransaction(context, persistenceUnitName);
    }

    public static void clearCaches() {
        STATE.clearCaches();
    }

    private static Mutiny.StatelessSession openStatelessSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();
        ContextLocals.put(PERSISTENCE_UNIT_NAME_KEY, persistenceUnitName);
        LOG.debugf("Opening lazy stateless session for Persistence Unit '%s'", persistenceUnitName);
        return STATE.createNewSession(persistenceUnitName, context);
    }
}
