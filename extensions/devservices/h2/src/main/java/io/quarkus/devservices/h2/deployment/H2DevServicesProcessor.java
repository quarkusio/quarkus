package io.quarkus.devservices.h2.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.h2.tools.Server;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

public class H2DevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(H2DevServicesProcessor.class);

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupH2() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.H2, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt port, LaunchMode launchMode, Optional<Duration> startupTimeout) {
                try {
                    final Server tcpServer = Server.createTcpServer("-tcpPort",
                            port.isPresent() ? String.valueOf(port.getAsInt()) : "0");
                    tcpServer.start();

                    StringBuilder additionalArgs = new StringBuilder();
                    for (Map.Entry<String, String> i : additionalProperties.entrySet()) {
                        additionalArgs.append(";");
                        additionalArgs.append(i.getKey());
                        additionalArgs.append("=");
                        additionalArgs.append(i.getValue());
                    }

                    LOG.info("Dev Services for H2 started.");

                    String connectionUrl = "jdbc:h2:tcp://localhost:" + tcpServer.getPort() + "/mem:"
                            + datasourceName.orElse("default")
                            + ";DB_CLOSE_DELAY=-1" + additionalArgs.toString();
                    return new RunningDevServicesDatasource(
                            connectionUrl,
                            "sa",
                            "sa",
                            new Closeable() {
                                @Override
                                public void close() throws IOException {
                                    //Test first, to not make too much noise if the Server is dead already
                                    //(perhaps we failed to start?)
                                    if (tcpServer.isRunning(false)) {
                                        //make sure the DB is removed on close
                                        try (Connection connection = DriverManager.getConnection(
                                                connectionUrl,
                                                "sa",
                                                "sa")) {
                                            try (Statement statement = connection.createStatement()) {
                                                statement.execute("SET DB_CLOSE_DELAY 0");
                                            }
                                        } catch (SQLException t) {
                                            t.printStackTrace();
                                        }
                                        tcpServer.stop();
                                        LOG.info("Dev Services for H2 shut down; server status: " + tcpServer.getStatus());
                                    } else {
                                        LOG.info(
                                                "Dev Services for H2 was NOT shut down as it appears it was down already; server status: "
                                                        + tcpServer.getStatus());
                                    }
                                }
                            });
                } catch (SQLException throwables) {
                    throw new RuntimeException(throwables);
                }
            }

            @Override
            public boolean isDockerRequired() {
                return false;
            }
        });
    }
}
