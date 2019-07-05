package io.quarkus.test.h2;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
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
            System.out.println("[INFO] H2 database started in TCP server mode");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return defaultH2Configuration();
    }

    private Map<String, String> defaultH2Configuration() {
        final Map<String, String> configuration = new HashMap<>();

        configuration.put("quarkus.datasource.url", "jdbc:h2:tcp://localhost/mem:test_quarkus;DB_CLOSE_DELAY=-1");
        configuration.put("quarkus.datasource.driver", "org.h2.Driver");
        configuration.put("quarkus.datasource.username", "sa");
        configuration.put("quarkus.datasource.password", "sa");

        return configuration;
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
