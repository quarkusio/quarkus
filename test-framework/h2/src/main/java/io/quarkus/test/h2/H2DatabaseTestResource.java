package io.quarkus.test.h2;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.h2.tools.Server;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class H2DatabaseTestResource implements QuarkusTestResourceLifecycleManager {

    private Server tcpServer;

    @Override
    public Map<String, String> start() {

        try {
            tcpServer = Server.createTcpServer();
            tcpServer.start();
            System.out.println("[INFO] H2 database started in TCP server mode; server status: " + tcpServer.getStatus());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    @Override
    public synchronized void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
            System.out.println("[INFO] H2 database was shut down; server status: " + tcpServer.getStatus());
            tcpServer = null;
        }
    }
}
