package io.quarkus.devservices.h2.deployment;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;
import org.hsqldb.server.ServerConfiguration;
import org.hsqldb.server.ServerConstants;
import org.hsqldb.server.ServerProperties;
import org.jboss.logging.Logger;

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

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.*;

public class HsqldbDevServicesProcessor {

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupHsqldb() {

        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.HSQLDB, new DevServicesDatasourceProvider() {

            @Override
            public RunningDevServicesDatasource startDatabase(
                    Optional<String> username,
                    Optional<String> password,
                    String datasourceName,
                    DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode,
                    Optional<Duration> startupTimeout
            ) {
                try {
                    // HSQLDB configuration.
                    ServerProperties fileProps = ServerConfiguration.newDefaultProperties(ServerConstants.SC_PROTOCOL_HSQL);

                    OptionalInt port = containerConfig.getFixedExposedPort();
                    String portS = String.valueOf(port.isPresent() ? port.getAsInt() : 0);
                    String effectiveUsername = containerConfig.getUsername().orElse(username.orElse(DEFAULT_DATABASE_USERNAME));
                    String effectivePassword = containerConfig.getPassword().orElse(password.orElse(DEFAULT_DATABASE_PASSWORD));
                    String effectiveDbName = containerConfig.getDbName().orElse(
                            DataSourceUtil.isDefault(datasourceName) ? DEFAULT_DATABASE_NAME : datasourceName);

                    // Start the HSQLDB instance.
                    LOG.info("Starting HSQLDB at port " + port + "...");
                    org.hsqldb.server.Server server = new org.hsqldb.server.Server();

                    // Format the JDBC URL.
                    StringBuilder additionalArgs = new StringBuilder();
                    for (Map.Entry<String, String> i : containerConfig.getAdditionalJdbcUrlProperties().entrySet()) {
                        additionalArgs.append(";");
                        additionalArgs.append(i.getKey());
                        additionalArgs.append("=");
                        additionalArgs.append(i.getValue());
                    }

                    String jdbcUrl = "jdbc:hsqldb:hsql://localhost:" + server.getPort() + "/mem:" + effectiveDbName + "" + additionalArgs.toString();
                    // In-memory would only work if something kept the connection the whole time. Not sure if Quarkus does it.
                    //String jdbcUrl = "jdbc:hsqldb:mem:" + effectiveDbName + "" + additionalArgs.toString();

                    LOG.info("Dev Services for HSQLDB started. Use JDBC URL: " + jdbcUrl);


                    return new RunningDevServicesDatasource(null, jdbcUrl, null, effectiveUsername, effectivePassword,
                            new Closeable() {

                                @Override
                                public void close() throws IOException {
                                    // Test first, to not make too much noise if the Server is dead already (perhaps we failed to start?)
                                    if (server.getState() == ServerConstants.SERVER_STATE_SHUTDOWN) {
                                        LOG.info("Dev Services for HSQLDB was NOT shut down as it appears it was down already; server status: " + server.getStateDescriptor());
                                        return;
                                    }

                                    // Make sure the DB is removed on close.
                                    try (Connection connection = DriverManager.getConnection(jdbcUrl, effectiveUsername, effectivePassword)) {
                                        try (Statement statement = connection.createStatement()) {
                                            statement.execute("DROP SCHEMA " + effectiveDbName + " CASCADE");
                                        }
                                    } catch (SQLException ex) {
                                        LOG.warn("Dev Services for HSQLDB could not drop schema " + effectiveDbName + ": " + ex.getMessage(), ex);
                                    }
                                    server.stop();
                                    for (int i = 0; i < 10; i++) {
                                        if (server.getState() == ServerConstants.SERVER_STATE_SHUTDOWN) break;
                                        try { Thread.sleep(100); }
                                        catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            break;
                                        }
                                    }
                                    LOG.info("Dev Services for HSQLDB shut down; server status: " + server.getStateDescriptor());
                                }
                            });
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override public boolean isDockerRequired() { return false; }
        });
    }


    private static final Logger LOG = Logger.getLogger(HsqldbDevServicesProcessor.class);
}