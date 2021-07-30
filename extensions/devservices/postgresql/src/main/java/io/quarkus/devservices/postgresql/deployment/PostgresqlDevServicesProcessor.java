package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;

public class PostgresqlDevServicesProcessor {

    public static final String TAG = "13.2";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupPostgres(
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.POSTGRESQL, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                PostgreSQLContainer container = new QuarkusPostgreSQLContainer(imageName, fixedExposedPort,
                        devServicesSharedNetworkBuildItem.isPresent())
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

    private static class QuarkusPostgreSQLContainer extends PostgreSQLContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusPostgreSQLContainer(Optional<String> imageName, OptionalInt fixedExposedPort, boolean useSharedNetwork) {
            super(DockerImageName.parse(imageName.orElse(PostgreSQLContainer.IMAGE + ":" + PostgresqlDevServicesProcessor.TAG))
                    .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE)));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "postgres");
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), PostgreSQLContainer.POSTGRESQL_PORT);
            }
        }

        @Override
        public String getJdbcUrl() {
            if (useSharedNetwork) {
                // in this case we expose the URL using the network alias we created in 'configure'
                // and the container port since the application communicating with this container
                // won't be doing port mapping
                String additionalUrlParams = constructUrlParameters("?", "&");
                return "jdbc:postgresql://" + hostName + ":" + POSTGRESQL_PORT
                        + "/" + getDatabaseName() + additionalUrlParams;
            } else {
                return super.getJdbcUrl();
            }
        }
    }
}
