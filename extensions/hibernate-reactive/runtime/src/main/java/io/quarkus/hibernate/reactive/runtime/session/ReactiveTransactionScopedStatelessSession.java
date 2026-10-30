package io.quarkus.hibernate.reactive.runtime.session;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.PERSISTENCE_UNIT_NAME_KEY;
import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.Optional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.delegation.MutinyStatelessSessionDelegator;
import org.jboss.logging.Logger;

import io.smallrye.common.vertx.ContextLocals;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class ReactiveTransactionScopedStatelessSession extends MutinyStatelessSessionDelegator {

    private static final Logger LOG = Logger.getLogger(ReactiveTransactionScopedStatelessSession.class);

    public static final OpenedSessionsState<Mutiny.StatelessSession> OPENED_SESSIONS_STATE_STATELESS = new OpenedSessionsStateStatelessImpl();

    private final String persistenceUnitName;

    public ReactiveTransactionScopedStatelessSession(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public Mutiny.StatelessSession delegate() {
        Context context = Vertx.currentContext();

        Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> openedSession = OPENED_SESSIONS_STATE_STATELESS
                .getOpenedSession(context, persistenceUnitName);
        if (openedSession.isPresent()) {
            return openedSession.get().session();
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
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> openedSession = OPENED_SESSIONS_STATE_STATELESS
                .getOpenedSession(context, persistenceUnitName);
        if (openedSession.isPresent()) {
            return openedSession.get().session();
        }
        return openStatelessSession(persistenceUnitName);
    }

    private static Mutiny.StatelessSession openStatelessSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();
        ContextLocals.put(PERSISTENCE_UNIT_NAME_KEY, persistenceUnitName);
        LOG.debugf("Opening lazy stateless session for Persistence Unit '%s'", persistenceUnitName);
        return OPENED_SESSIONS_STATE_STATELESS.createNewSession(persistenceUnitName, context);
    }
}
