package io.quarkus.devservices.postgresql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;

public class MariaDBDevServicesProcessor {

    public static final String TAG = "10.5.9";
    public static final Integer PORT = 3306;

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMariaDB(
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MARIADB, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                MariaDBContainer container = new QuarkusMariaDBContainer(imageName, fixedExposedPort,
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

    private static class QuarkusMariaDBContainer extends MariaDBContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusMariaDBContainer(Optional<String> imageName, OptionalInt fixedExposedPort, boolean useSharedNetwork) {
            super(DockerImageName.parse(imageName.orElse(MariaDBContainer.IMAGE + ":" + MariaDBDevServicesProcessor.TAG))
                    .asCompatibleSubstituteFor(DockerImageName.parse(MariaDBContainer.IMAGE)));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "mariadb");
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), PORT);
            }
        }

        @Override
        public String getJdbcUrl() {
            if (useSharedNetwork) {
                String additionalUrlParams = constructUrlParameters("?", "&");
                return "jdbc:mariadb://" + hostName + ":" + PORT + "/" + getDatabaseName() + additionalUrlParams;
            } else {
                return super.getJdbcUrl();
            }
        }
    }
}
