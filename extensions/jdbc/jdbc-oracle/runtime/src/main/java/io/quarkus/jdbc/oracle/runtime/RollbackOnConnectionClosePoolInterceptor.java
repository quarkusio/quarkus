package io.quarkus.jdbc.oracle.runtime;

import java.sql.Connection;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.pool.wrapper.ConnectionWrapper;
import oracle.jdbc.OracleConnection;

/**
 * Oracle has the weird behavior that it performs an implicit commit on normal connection closure (even with auto-commit
 * disabled),
 * which happens on application shutdown. To prevent whatever intermittent state we have during shutdown from being committed,
 * we add an explicit rollback to each connection closure. If the connection has already received a COMMIT, the rollback will
 * not work, which is fine.
 * <p>
 * The code unwraps the {@link Connection} so that we perform the rollback directly on the underlying database connection,
 * and not on e.g. Agroal's {@link ConnectionWrapper} which can prevent the rollback from actually being executed due to some
 * safeguards.
 *
 * @see https://github.com/quarkusio/quarkus/issues/36265
 */
public class RollbackOnConnectionClosePoolInterceptor implements AgroalPoolInterceptor {

    private static final Logger LOG = Logger.getLogger(RollbackOnConnectionClosePoolInterceptor.class);

    private final boolean noAutomaticRollback;

    public RollbackOnConnectionClosePoolInterceptor() {
        // if you have to use this system property, make sure you open an issue in the Quarkus tracker to explain
        // why as we might need to adjust things
        noAutomaticRollback = Boolean.getBoolean("quarkus-oracle-no-automatic-rollback-on-connection-close");
    }

    @Override
    public void onConnectionDestroy(Connection connection) {
        if (noAutomaticRollback) {
            return;
        }

        // do not rollback XA connections, they are handled by the transaction manager
        if (connection instanceof ConnectionWrapper connectionWrapper) {
            if (connectionWrapper.getHandler().getXaResource() != null) {
                return;
            }
        }

        try {
            if (connection.unwrap(Connection.class) instanceof OracleConnection oracleConnection) {
                if (connection.isClosed() || connection.getAutoCommit()) {
                    return;
                }

                oracleConnection.rollback();
            }
        } catch (Exception e) {
            LOG.trace("Ignoring exception during rollback on connection close", e);
        }
    }
}
