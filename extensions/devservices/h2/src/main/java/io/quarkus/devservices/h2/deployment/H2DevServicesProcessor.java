package io.quarkus.devservices.h2.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.h2.tools.Server;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DatasourceStartable;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.runtime.LaunchMode;

public class H2DevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(H2DevServicesProcessor.class);

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupH2() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.H2, new DevServicesDatasourceProvider() {
            @Override
            public String getFeature() {
                return Feature.JDBC_H2.getName();
            }

            @Override
            public DatasourceStartable createDatasourceStartable(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, boolean useSharedNetwork, Optional<Duration> startupTimeout) {
                try {
                    final Server tcpServer = Server.createTcpServer("-tcpPort",
                            containerConfig.getFixedExposedPort().isPresent()
                                    ? String.valueOf(containerConfig.getFixedExposedPort().getAsInt())
                                    : "0",
                            "-ifNotExists");

                    String effectiveUsername = containerConfig.getUsername().orElse(username.orElse(DEFAULT_DATABASE_USERNAME));
                    String effectivePassword = containerConfig.getPassword().orElse(password.orElse(DEFAULT_DATABASE_PASSWORD));
                    String effectiveDbName = containerConfig.getDbName().orElse(
                            DataSourceUtil.isDefault(datasourceName) ? DEFAULT_DATABASE_NAME : datasourceName);

                    StringBuilder additionalArgs = new StringBuilder();
                    for (Map.Entry<String, String> i : containerConfig.getAdditionalJdbcUrlProperties().entrySet()) {
                        additionalArgs.append(";");
                        additionalArgs.append(i.getKey());
                        additionalArgs.append("=");
                        additionalArgs.append(i.getValue());
                    }

                    return new DatasourceStartable() {
                        private String connectionUrl;

                        @Override
                        public String getPassword() {
                            return effectivePassword;
                        }

                        @Override
                        public String getUsername() {
                            return effectiveUsername;
                        }

                        @Override
                        public String getReactiveUrl() {
                            return null;
                        }

                        @Override
                        public String getEffectiveJdbcUrl() {
                            return connectionUrl;
                        }

                        @Override
                        public void start() {
                            try {
                                tcpServer.start();
                                connectionUrl = "jdbc:h2:tcp://localhost:" + tcpServer.getPort() + "/mem:"
                                        + effectiveDbName
                                        + ";DB_CLOSE_DELAY=-1" + additionalArgs;
                            } catch (SQLException throwables) {
                                throw new RuntimeException(throwables);
                            }
                        }

                        @Override
                        public void close() {
                            //Test first, to not make too much noise if the Server is dead already
                            //(perhaps we failed to start?)
                            if (tcpServer.isRunning(false)) {
                                //make sure the DB is removed on close
                                try (Connection connection = DriverManager.getConnection(
                                        connectionUrl,
                                        effectiveUsername,
                                        effectivePassword)) {
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

                        @Override
                        public String getConnectionInfo() {
                            return getEffectiveJdbcUrl();
                        }

                        @Override
                        public String getContainerId() {
                            return null;
                        }

                        @Override
                        public DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevServicesDatasource() {
                            // Override, so we can check the connection url instead of the container id
                            if (connectionUrl == null) {
                                throw new IllegalStateException(
                                        "Cannot create a RunningDevServicesDatasource before the H2 datasource has been started.");
                            }
                            // It would be nice to cache this, but since this is an interface, it can't be done at this level, and would have to be done by every implementing class
                            return new DevServicesDatasourceProvider.RunningDevServicesDatasource(getContainerId(),
                                    getEffectiveJdbcUrl(),
                                    getReactiveUrl(), getUsername(), getPassword());
                        }

                    };
                } catch (SQLException throwables) {
                    throw new RuntimeException(throwables);
                }
            }

            @Override
            public Optional<DevServicesDatasourceProvider.RunningDevServicesDatasource> findRunningComposeDatasource(
                    LaunchMode launchMode, boolean useSharedNetwork, DevServicesDatasourceContainerConfig containerConfig,
                    DevServicesComposeProjectBuildItem composeProjectBuildItem) {
                return Optional.empty();
            }

            @Override
            public boolean isDockerRequired() {
                return false;
            }
        });
    }
}
