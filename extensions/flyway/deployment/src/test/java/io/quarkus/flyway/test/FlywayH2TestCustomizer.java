package io.quarkus.flyway.test;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import org.h2.tools.RunScript;
import org.h2.tools.Server;

public class FlywayH2TestCustomizer {
    private String initSqlFile;
    private String dbName;
    private int port = ThreadLocalRandom.current().nextInt(49152, 65535);
    private Server tcpServer;

    protected FlywayH2TestCustomizer() {
    }

    private FlywayH2TestCustomizer(String dbName) {
        this.dbName = dbName;
    }

    public static FlywayH2TestCustomizer withDbName(String dbName) {
        return new FlywayH2TestCustomizer(dbName);
    }

    public FlywayH2TestCustomizer withPort(int port) {
        this.port = port;
        return this;
    }

    public FlywayH2TestCustomizer withInitSqlFile(String initSqlFile) {
        this.initSqlFile = initSqlFile;
        return this;
    }

    void startH2() {
        try {
            tcpServer = Server.createTcpServer("-tcpPort", String.valueOf(port));
            tcpServer.start();
            System.out.println("[INFO] Custom H2 database started in TCP server mode; server status: " + tcpServer.getStatus());
            if (initSqlFile != null) {
                executeInitSQL();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void executeInitSQL() {
        requireNonNull(dbName, "Flyway-H2: Default db for init-sql must be specified!");
        requireNonNull(initSqlFile, "Flyway-H2: init-sql must be specified!");
        final String url = buildDbURL();
        try {
            System.out.println("[INFO] Custom H2 Intializing DB: " + url);
            RunScript.execute(
                    url,
                    "sa",
                    "sa",
                    initSqlFile,
                    Charset.defaultCharset(),
                    false);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public String buildDbURL() {
        return "jdbc:h2:tcp://localhost:" + port + "/mem:" + dbName + ";DB_CLOSE_DELAY=-1";
    }

    public void stopH2() {
        if (tcpServer != null) {
            tcpServer.stop();
            System.out.println("[INFO] Custom H2 database was shut down; server status: " + tcpServer.getStatus());
            tcpServer = null;
        }
    }
}
