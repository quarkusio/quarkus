package io.quarkus.devservices.mysql.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.Volumes;
import io.quarkus.runtime.LaunchMode;

public class MySQLDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(MySQLDevServicesProcessor.class);

    public static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMysql(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MYSQL, new DevServicesDatasourceProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, Optional<Duration> startupTimeout) {
                QuarkusMySQLContainer container = new QuarkusMySQLContainer(containerConfig.getImageName(),
                        containerConfig.getFixedExposedPort(),
                        !devServicesSharedNetworkBuildItem.isEmpty());
                startupTimeout.ifPresent(container::withStartupTimeout);

                String effectiveUsername = containerConfig.getUsername().orElse(username.orElse(DEFAULT_DATABASE_USERNAME));
                String effectivePassword = containerConfig.getPassword().orElse(password.orElse(DEFAULT_DATABASE_PASSWORD));
                String effectiveDbName = containerConfig.getDbName().orElse(
                        DataSourceUtil.isDefault(datasourceName) ? DEFAULT_DATABASE_NAME : datasourceName);

                container.withUsername(effectiveUsername)
                        .withPassword(effectivePassword)
                        .withDatabaseName(effectiveDbName)
                        .withReuse(true);
                Labels.addDataSourceLabel(container, datasourceName);
                Volumes.addVolumes(container, containerConfig.getVolumes());

                container.withEnv(containerConfig.getContainerEnv());

                if (containerConfig.getContainerProperties().containsKey(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME)) {
                    container.withConfigurationOverride(
                            containerConfig.getContainerProperties().get(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME));
                }

                containerConfig.getAdditionalJdbcUrlProperties().forEach(container::withUrlParam);
                containerConfig.getCommand().ifPresent(container::setCommand);
                containerConfig.getInitScriptPath().ifPresent(container::withInitScript);

                container.start();

                LOG.info("Dev Services for MySQL started.");

                return new RunningDevServicesDatasource(container.getContainerId(),
                        container.getEffectiveJdbcUrl(),
                        container.getReactiveUrl(),
                        container.getUsername(),
                        container.getPassword(),
                        new ContainerShutdownCloseable(container, "MySQL"));
            }
        });
    }

    private static class QuarkusMySQLContainer extends MySQLContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusMySQLContainer(Optional<String> imageName, OptionalInt fixedExposedPort, boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mysql")))
                    .asCompatibleSubstituteFor(DockerImageName.parse(MySQLContainer.NAME)));
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
                addFixedExposedPort(fixedExposedPort.getAsInt(), MySQLContainer.MYSQL_PORT);
            } else {
                addExposedPort(MYSQL_PORT);
            }
        }

        // this is meant to be called by Quarkus code and is needed in order to not disrupt testcontainers
        // from being able to determine the status of the container (which it does by trying to acquire a connection)
        public String getEffectiveJdbcUrl() {
            if (useSharedNetwork) {
                // in this case we expose the URL using the network alias we created in 'configure'
                // and the container port since the application communicating with this container
                // won't be doing port mapping
                String additionalUrlParams = constructUrlParameters("?", "&");
                return "jdbc:mysql://" + hostName + ":" + MYSQL_PORT + "/" + getDatabaseName() + additionalUrlParams;
            } else {
                return super.getJdbcUrl();
            }
        }

        public String getReactiveUrl() {
            return getEffectiveJdbcUrl().replaceFirst("jdbc:", "vertx-reactive:");
        }
    }
}
