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
                StringBuilder additionalArgs = new StringBuilder();
                for (Map.Entry<String, String> i : additionalProperties.entrySet()) {
                    if (additionalArgs.length() > 0) {
                        additionalArgs.append("&");
                    }
                    additionalArgs.append(i.getKey());
                    additionalArgs.append("=");
                    additionalArgs.append(i.getValue());
                }
                String jdbcUrl = container.getJdbcUrl();
                if (additionalArgs.length() > 0) {
                    if (jdbcUrl.contains("?")) {
                        jdbcUrl = jdbcUrl + "&" + additionalArgs.toString();
                    } else {
                        jdbcUrl = jdbcUrl + "?" + additionalArgs.toString();
                    }
                }
                return new RunningDevServicesDatasource(jdbcUrl, container.getUsername(),
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
