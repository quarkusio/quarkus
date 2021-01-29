package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;

import org.apache.derby.drda.NetworkServerControl;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.devservices.DevServicesDatasourceProviderBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.deployment.annotations.BuildStep;

public class DerbyDevServicesProcessor {

    static final int NUMBER_OF_PINGS = 10;
    static final int SLEEP_BETWEEN_PINGS = 500;

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupDerby() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.DERBY, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties) {
                try {
                    NetworkServerControl server = new NetworkServerControl();
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
                    return new RunningDevServicesDatasource(
                            "jdbc:derby://localhost:1527/memory:" + datasourceName.orElse("quarkus") + ";create=true", null,
                            null,
                            new Closeable() {
                                @Override
                                public void close() throws IOException {
                                    try {
                                        NetworkServerControl server = new NetworkServerControl();
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
        });
    }
}
