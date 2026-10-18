package io.quarkus.it.opentelemetry;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.h2.tools.Server;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class H2DatabaseTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String QUARKUS_OTEL_SDK_DISABLED = "quarkus.otel.sdk.disabled";
    private Server tcpServer;
    private Map<String, String> initProperties;

    @Override
    public void init(Map<String, String> initArgs) {
        initProperties = initArgs;
    }

    @Override
    public Map<String, String> start() {

        try {
            tcpServer = Server.createTcpServer("-ifNotExists");
            tcpServer.start();
            System.out.println("[INFO] H2 database started in TCP server mode; server status: " + tcpServer.getStatus());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> properties = new HashMap<>(initProperties);
        properties.put("quarkus.datasource.h2.jdbc.url", "jdbc:h2:tcp://localhost/mem:test");
        properties.put("quarkus.hibernate-orm.h2.schema-management.strategy", "drop-and-create");
        properties.put("quarkus.hibernate-orm.postgresql.active", "false");
        properties.put("quarkus.hibernate-orm.oracle.active", "false");
        properties.put("quarkus.hibernate-orm.mariadb.active", "false");
        properties.put("quarkus.hibernate-orm.db2.active", "false");

        return properties;
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
