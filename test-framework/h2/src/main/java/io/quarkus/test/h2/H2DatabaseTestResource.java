package io.quarkus.test.h2;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.h2.tools.Server;
import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class H2DatabaseTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(H2DatabaseTestResource.class);

    private Server tcpServer;

    @Override
    public Map<String, String> start() {

        try {
            tcpServer = Server.createTcpServer("-ifNotExists");
            tcpServer.start();
            log.infof("H2 database started in TCP server mode; server status: %s", tcpServer.getStatus());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    @Override
    public synchronized void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
            log.infof("H2 database was shut down; server status: %s", tcpServer.getStatus());
            tcpServer = null;
        }
    }
}
