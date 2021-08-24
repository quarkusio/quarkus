package io.quarkus.devservices.mssql.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;

public class MSSQLDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(MSSQLDevServicesProcessor.class);

    /**
     * If you update this remember to update the container-license-acceptance.txt in the tests
     */
    public static final String TAG = "2019-CU10-ubuntu-20.04";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMSSQL(
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MSSQL, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                JdbcDatabaseContainer container = new QuarkusMSSQLServerContainer(imageName, fixedExposedPort,
                        devServicesSharedNetworkBuildItem.isPresent())
                                .withPassword(password.orElse("Quarkuspassword1"));
                additionalProperties.forEach(container::withUrlParam);
                container.start();

                LOG.info("Dev Services for Microsoft SQL Server started.");

                return new RunningDevServicesDatasource(container.getJdbcUrl(), container.getUsername(),
                        container.getPassword(),
                        new Closeable() {
                            @Override
                            public void close() throws IOException {
                                container.stop();

                                LOG.info("Dev Services for Microsoft SQL Server shut down.");
                            }
                        });
            }
        });
    }

    private static class QuarkusMSSQLServerContainer extends MSSQLServerContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusMSSQLServerContainer(Optional<String> imageName, OptionalInt fixedExposedPort, boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElse(MSSQLServerContainer.IMAGE + ":" + MSSQLDevServicesProcessor.TAG))
                    .asCompatibleSubstituteFor(MSSQLServerContainer.IMAGE));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "mssql");
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), MSSQLServerContainer.MS_SQL_SERVER_PORT);
            }
        }

        @Override
        public String getJdbcUrl() {
            if (useSharedNetwork) {
                // in this case we expose the URL using the network alias we created in 'configure'
                // and the container port since the application communicating with this container
                // won't be doing port mapping
                String additionalUrlParams = constructUrlParameters(";", ";");
                return "jdbc:sqlserver://" + hostName + ":" + MS_SQL_SERVER_PORT + additionalUrlParams;
            } else {
                return super.getJdbcUrl();
            }
        }
    }
}
