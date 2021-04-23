package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class PostgresqlDevServicesProcessor {

    public static final String TAG = "13.2";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupPostgres() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.POSTGRESQL, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties) {
                PostgreSQLContainer container = new PostgreSQLContainer(
                        DockerImageName.parse(imageName.orElse(PostgreSQLContainer.IMAGE + ":" + TAG))
                                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE)))
                                        .withPassword(password.orElse("quarkus"))
                                        .withUsername(username.orElse("quarkus"))
                                        .withDatabaseName(datasourceName.orElse("default"));
                additionalProperties.forEach(container::withUrlParam);
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
