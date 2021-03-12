package io.quarkus.devservices.mssql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class MSSQLDevServicesProcessor {

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMSSQL() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MSSQL, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties) {
                JdbcDatabaseContainer container = new MSSQLServerContainer(
                        imageName.orElse(MSSQLServerContainer.IMAGE + ":" + MSSQLServerContainer.DEFAULT_TAG))
                                .withPassword(password.orElse("Quarkuspassword1"));
                container.start();
                return new RunningDevServicesDatasource(container.getJdbcUrl(), container.getUsername(),
                        container.getPassword(),
                        new Closeable() {
                            @Override
                            public void close() throws IOException {
                                container.stop();
                            }
                        });
            }
        });
    }

}
