package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.apache.derby.drda.NetworkServerControl;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

public class DerbyDevServicesProcessor {

    static final int NUMBER_OF_PINGS = 10;
    static final int SLEEP_BETWEEN_PINGS = 500;

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupDerby() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.DERBY, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                try {
                    int port = fixedExposedPort.isPresent() ? fixedExposedPort.getAsInt()
                            : 1527 + (launchMode == LaunchMode.TEST ? 0 : 1);
                    NetworkServerControl server = new NetworkServerControl(InetAddress.getByName("localhost"), port);
                    server.start(new PrintWriter(System.out));
                    for (int i = 1; i <= NUMBER_OF_PINGS; i++) {
                        try {
                            System.out.println("[INFO] Attempt " + i + " to see if Derby Network server started");
                            server.ping();
                            break;
                        } catch (Exception ex) {
                            if (i == NUMBER_OF_PINGS) {
                                System.out.println("Derby Network server failed to start");
                                ex.printStackTrace();
                                throw ex;
                            }
                            try {
                                Thread.sleep(SLEEP_BETWEEN_PINGS);
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }
                    StringBuilder additionalArgs = new StringBuilder();
                    for (Map.Entry<String, String> i : additionalProperties.entrySet()) {
                        additionalArgs.append(";");
                        additionalArgs.append(i.getKey());
                        additionalArgs.append("=");
                        additionalArgs.append(i.getValue());
                    }
                    return new RunningDevServicesDatasource(
                            "jdbc:derby://localhost:" + port + "/memory:" + datasourceName.orElse("quarkus") + ";create=true"
                                    + additionalArgs.toString(),
                            null,
                            null,
                            new Closeable() {
                                @Override
                                public void close() throws IOException {
                                    try {
                                        NetworkServerControl server = new NetworkServerControl(
                                                InetAddress.getByName("localhost"), port);
                                        server.shutdown();
                                        System.out.println("[INFO] Derby database was shut down");
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
