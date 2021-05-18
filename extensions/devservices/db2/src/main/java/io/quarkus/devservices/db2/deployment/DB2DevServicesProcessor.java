package io.quarkus.devservices.db2.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

public class DB2DevServicesProcessor {

    /**
     * If you update this remember to update the container-license-acceptance.txt in the tests
     */
    public static final String TAG = "11.5.5.1";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupDB2() {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.DB2, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                Db2Container container = new Db2Container(
                        DockerImageName.parse(imageName.orElse("ibmcom/db2:" + TAG))
                                .asCompatibleSubstituteFor(DockerImageName.parse("ibmcom/db2"))) {
                    @Override
                    protected void configure() {
                        super.configure();
                        if (fixedExposedPort.isPresent()) {
                            addFixedExposedPort(fixedExposedPort.getAsInt(), DB2_PORT);
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
