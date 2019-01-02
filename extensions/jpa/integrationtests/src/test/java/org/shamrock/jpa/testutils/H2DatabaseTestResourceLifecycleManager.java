package org.shamrock.jpa.testutils;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.jboss.shamrock.test.ShamrockTestResource;
import org.jboss.shamrock.test.ShamrockTestResourceLifecycleManager;

@ShamrockTestResource(H2DatabaseTestResourceLifecycleManager.class)
public class H2DatabaseTestResourceLifecycleManager implements ShamrockTestResourceLifecycleManager {

    private Server tcpServer;

    @Override
    public synchronized void start() {
        try {
            tcpServer = Server.createTcpServer();
            tcpServer.start();
            System.out.println("[INFO] H2 database started in TCP server mode");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
            System.out.println("[INFO] H2 database was shut down");
            tcpServer = null;
        }
    }
}
