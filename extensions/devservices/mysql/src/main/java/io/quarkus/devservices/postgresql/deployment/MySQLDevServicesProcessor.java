package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

public class MySQLDevServicesProcessor {

    public static final String TAG = "8.0.24";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMysql() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MYSQL, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                MySQLContainer container = new MySQLContainer(
                        DockerImageName.parse(imageName.orElse(MySQLContainer.IMAGE + ":" + TAG))
                                .asCompatibleSubstituteFor(DockerImageName.parse(MySQLContainer.IMAGE))) {
                    @Override
                    protected void configure() {
                        super.configure();
                        if (fixedExposedPort.isPresent()) {
                            addFixedExposedPort(fixedExposedPort.getAsInt(), MySQLContainer.MYSQL_PORT);
                        }
                    };
                }
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
