package io.quarkus.devservices.derby.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.apache.derby.drda.NetworkServerControl;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

public class DerbyDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(DerbyDevServicesProcessor.class);

    static final int NUMBER_OF_PINGS = 10;
    static final int SLEEP_BETWEEN_PINGS = 500;

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupDerby() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.DERBY, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, Optional<Duration> startupTimeout) {
                try {
                    int port = containerConfig.getFixedExposedPort().isPresent()
                            ? containerConfig.getFixedExposedPort().getAsInt()
                            : 1527 + (launchMode == LaunchMode.TEST ? 0 : 1);

                    String effectiveDbName = containerConfig.getDbName().orElse(datasourceName.orElse(DEFAULT_DATABASE_NAME));

                    NetworkServerControl server = new NetworkServerControl(InetAddress.getByName("localhost"), port);
                    server.start(new PrintWriter(System.out));
                    for (int i = 1; i <= NUMBER_OF_PINGS; i++) {
                        try {
                            LOG.info("Attempt " + i + " to see if Dev Services for Derby started");
                            server.ping();
                            break;
                        } catch (Exception ex) {
                            if (i == NUMBER_OF_PINGS) {
                                LOG.error("Dev Services for Derby failed to start", ex);
                                throw ex;
                            }
                            try {
                                Thread.sleep(SLEEP_BETWEEN_PINGS);
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }

                    LOG.info("Dev Services for Derby started.");

                    StringBuilder additionalArgs = new StringBuilder();
                    for (Map.Entry<String, String> i : containerConfig.getAdditionalJdbcUrlProperties().entrySet()) {
                        additionalArgs.append(";");
                        additionalArgs.append(i.getKey());
                        additionalArgs.append("=");
                        additionalArgs.append(i.getValue());
                    }
                    return new RunningDevServicesDatasource(null,
                            "jdbc:derby://localhost:" + port + "/memory:" + effectiveDbName + ";create=true"
                                    + additionalArgs.toString(),
                            null,
                            null,
                            null,
                            new Closeable() {
                                @Override
                                public void close() throws IOException {
                                    try {
                                        NetworkServerControl server = new NetworkServerControl(
                                                InetAddress.getByName("localhost"), port);
                                        server.shutdown();
                                        LOG.info("Dev Services for Derby shut down");
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                } catch (Exception throwable) {
                    throw new RuntimeException(throwable);
                }
            }

            @Override
            public boolean isDockerRequired() {
                return false;
            }
        });
    }
}
