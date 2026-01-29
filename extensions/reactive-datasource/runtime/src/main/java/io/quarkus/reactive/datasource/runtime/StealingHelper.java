package io.quarkus.reactive.datasource.runtime;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.impl.ContextInternal;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.impl.Connection;
import io.vertx.sqlclient.impl.SocketConnectionBase;
import io.vertx.sqlclient.impl.SqlConnectionInternal;

@Singleton
public class StealingHelper {
    private static final Logger log = Logger.getLogger(StealingHelper.class);

    private final Instance<ConnectionStealingMonitor> monitors;

    @Inject
    public StealingHelper(Instance<ConnectionStealingMonitor> monitors) {
        this.monitors = monitors;
    }

    public Future<SqlConnection> wrap(Future<SqlConnection> future, String datasourceName) {
        ContextInternal callerContext = ContextInternal.current();
        // We're only interested in measuring changes in EL threads
        if (callerContext == null || !callerContext.isEventLoopContext()) {
            return future;
        }
        return future.onSuccess(conn -> {
            if (conn instanceof SqlConnectionInternal connInternal) {
                boolean stolen = false;
                Connection connection = connInternal
                        // Unwrap the pooled connection
                        .unwrap()
                        // Unwrap the base connection
                        .unwrap();
                if (connection instanceof SocketConnectionBase socketConnection) {
                    ContextInternal connContext = (ContextInternal) socketConnection.context();
                    if (connContext.nettyEventLoop() != callerContext.nettyEventLoop()) {
                        stolen = true;
                    }
                }
                notifyMonitors(datasourceName, stolen);
            }
        });
    }

    private void notifyMonitors(String datasourceName, boolean stolen) {
        if (monitors.isUnsatisfied()) {
            if (stolen)
                log.warnf("Connection stealing detected in datasource '%s'!", datasourceName);
        } else {
            for (ConnectionStealingMonitor monitor : monitors) {
                try {
                    monitor.connectionAcquired(datasourceName, stolen);
                } catch (Throwable t) {
                    log.debug("Monitor failed", t);
                }
            }
        }
    }
}