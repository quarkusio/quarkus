package io.quarkus.devservices.mssql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

public class MSSQLDevServicesProcessor {

    /**
     * If you update this remember to update the container-license-acceptance.txt in the tests
     */
    public static final String TAG = "2019-CU10-ubuntu-20.04";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMSSQL() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MSSQL, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                JdbcDatabaseContainer container = new MSSQLServerContainer(
                        DockerImageName
                                .parse(imageName.orElse(MSSQLServerContainer.IMAGE + ":" + TAG))
                                .asCompatibleSubstituteFor(MSSQLServerContainer.IMAGE)) {
                    @Override
                    protected void configure() {
                        super.configure();
                        if (fixedExposedPort.isPresent()) {
                            addFixedExposedPort(fixedExposedPort.getAsInt(), MSSQLServerContainer.MS_SQL_SERVER_PORT);
                        }
                    };
                }
                        .withPassword(password.orElse("Quarkuspassword1"));
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
