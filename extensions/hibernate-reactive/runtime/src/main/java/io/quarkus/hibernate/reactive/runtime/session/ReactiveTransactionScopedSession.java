package io.quarkus.hibernate.reactive.runtime.session;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.PERSISTENCE_UNIT_NAME_KEY;
import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.Optional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.delegation.MutinySessionDelegator;
import org.jboss.logging.Logger;

import io.smallrye.common.vertx.ContextLocals;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class ReactiveTransactionScopedSession extends MutinySessionDelegator {

    private static final Logger LOG = Logger.getLogger(ReactiveTransactionScopedSession.class);

    public static final OpenedSessionsState<Mutiny.Session> OPENED_SESSIONS_STATE = new OpenedSessionsStateStatefulImpl();

    private final String persistenceUnitName;

    public ReactiveTransactionScopedSession(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public Mutiny.Session delegate() {
        Context context = Vertx.currentContext();

        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedSession = OPENED_SESSIONS_STATE
                .getOpenedSession(context, persistenceUnitName);
        if (openedSession.isPresent()) {
            return openedSession.get().session();
        } else if (ContextLocals.get(TRANSACTIONAL_METHOD_KEY).isEmpty()) {
            throw new IllegalStateException(
                    "No current Mutiny.Session found"
                            + "\n\t- you need to annotate your method with @Transactional to open a reactive session"
                            + "\n\t- alternatively, you can use @WithSessionOnDemand, @WithSession, or @WithTransaction"
                            + "\n\t- for JAX-RS resources, annotate the method directly with an HTTP method (@GET, @POST, etc.) to automatically open a session");
        } else {
            return openSession(persistenceUnitName);
        }
    }

    /**
     * Returns an existing session for the given persistence unit, or opens a new one.
     */
    public static Mutiny.Session getOrOpenSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedSession = OPENED_SESSIONS_STATE
                .getOpenedSession(context, persistenceUnitName);
        if (openedSession.isPresent()) {
            return openedSession.get().session();
        }
        return openSession(persistenceUnitName);
    }

    private static Mutiny.Session openSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();
        ContextLocals.put(PERSISTENCE_UNIT_NAME_KEY, persistenceUnitName);
        LOG.debugf("Opening lazy session for Persistence Unit '%s'", persistenceUnitName);
        return OPENED_SESSIONS_STATE.createNewSession(persistenceUnitName, context);
    }
}
