package io.quarkus.devservices.oracle.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;

public class OracleDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(OracleDevServicesProcessor.class);

    public static final String IMAGE = "gvenzl/oracle-xe";
    public static final String TAG = "18.4.0-slim";
    public static final String DEFAULT_DATABASE_USER = "quarkus";
    public static final String DEFAULT_DATABASE_PASSWORD = "quarkus";
    public static final String DEFAULT_DATABASE_NAME = "quarkusdb";
    public static final int PORT = 1521;

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupOracle(
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.ORACLE, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    Optional<String> datasourceName, Optional<String> imageName, Map<String, String> additionalProperties,
                    OptionalInt fixedExposedPort, LaunchMode launchMode) {
                QuarkusOracleServerContainer container = new QuarkusOracleServerContainer(imageName, fixedExposedPort,
                        devServicesSharedNetworkBuildItem.isPresent());
                container.withUsername(username.orElse(DEFAULT_DATABASE_USER))
                        .withPassword(password.orElse(DEFAULT_DATABASE_PASSWORD))
                        .withDatabaseName(datasourceName.orElse(DEFAULT_DATABASE_NAME));
                additionalProperties.forEach(container::withUrlParam);
                container.start();

                LOG.info("Dev Services for Oracle started.");

                return new RunningDevServicesDatasource(container.getEffectiveJdbcUrl(), container.getUsername(),
                        container.getPassword(),
                        new Closeable() {
                            @Override
                            public void close() throws IOException {
                                container.stop();

                                LOG.info("Dev Services for Oracle shut down.");
                            }
                        });
            }
        });
    }

    private static class QuarkusOracleServerContainer extends OracleContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusOracleServerContainer(Optional<String> imageName, OptionalInt fixedExposedPort,
                boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElse(OracleDevServicesProcessor.IMAGE + ":" + OracleDevServicesProcessor.TAG))
                    .asCompatibleSubstituteFor(OracleDevServicesProcessor.IMAGE));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "oracle");
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), OracleDevServicesProcessor.PORT);
            }
        }

        // this is meant to be called by Quarkus code and is needed in order to not disrupt testcontainers
        // from being able to determine the status of the container (which it does by trying to acquire a connection)
        public String getEffectiveJdbcUrl() {
            if (useSharedNetwork) {
                // in this case we expose the URL using the network alias we created in 'configure'
                // and the container port since the application communicating with this container
                // won't be doing port mapping
                return "jdbc:oracle:thin//" + hostName + ":" + PORT + ":" + getDatabaseName();
            } else {
                return super.getJdbcUrl();
            }
        }
    }
}