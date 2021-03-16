package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;

import org.h2.tools.Server;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class H2DevServicesProcessor {

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupH2() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.H2, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties) {
                try {
                    final Server tcpServer = Server.createTcpServer("-tcpPort", "0");
                    tcpServer.start();
                    System.out
                            .println("[INFO] H2 database started in TCP server mode; server status: " + tcpServer.getStatus());
                    String connectionUrl = "jdbc:h2:tcp://localhost:" + tcpServer.getPort() + "/mem:"
                            + datasourceName.orElse("default")
                            + ";DB_CLOSE_DELAY=-1";
                    return new RunningDevServicesDatasource(
                            connectionUrl,
                            "sa",
                            "sa",
                            new Closeable() {
                                @Override
                                public void close() throws IOException {
                                    //make sure the DB is removed on close
                                    try (Connection connection = DriverManager.getConnection(connectionUrl, "sa", "sa")) {
                                        try (Statement statement = connection.createStatement()) {
                                            statement.execute("SET DB_CLOSE_DELAY 0");
                                        }
                                    } catch (SQLException t) {
                                        t.printStackTrace();
                                    }
                                    tcpServer.stop();
                                    System.out.println(
                                            "[INFO] H2 database was shut down; server status: " + tcpServer.getStatus());
                                }
                            });
                } catch (SQLException throwables) {
                    throw new RuntimeException(throwables);
                }
            }
        });
    }
}
