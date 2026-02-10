package io.quarkus.devservices.mariadb.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DatasourceStartable;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.Volumes;
import io.quarkus.runtime.LaunchMode;

public class MariaDBDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(MariaDBDevServicesProcessor.class);

    public static final Integer PORT = 3306;
    public static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";

    private static final MariaDBDatasourceServiceConfigurator configurator = new MariaDBDatasourceServiceConfigurator();

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMariaDB(
            DevServicesComposeProjectBuildItem composeProjectBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MARIADB, new DevServicesDatasourceProvider() {
            @Override
            public String getFeature() {
                return Feature.JDBC_MARIADB.getName();
            }

            @Override
            public DatasourceStartable createDatasourceStartable(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, boolean useSharedNetwork, Optional<Duration> startupTimeout) {

                String effectiveUsername = containerConfig.getUsername().orElse(username.orElse(DEFAULT_DATABASE_USERNAME));
                String effectivePassword = containerConfig.getPassword().orElse(password.orElse(DEFAULT_DATABASE_PASSWORD));
                String effectiveDbName = containerConfig.getDbName().orElse(
                        DataSourceUtil.isDefault(datasourceName) ? DEFAULT_DATABASE_NAME : datasourceName);

                QuarkusMariaDBContainer container = new QuarkusMariaDBContainer(containerConfig.getImageName(),
                        containerConfig.getFixedExposedPort(),
                        composeProjectBuildItem.getDefaultNetworkId(),
                        useSharedNetwork);
                startupTimeout.ifPresent(container::withStartupTimeout);

                container.withUsername(effectiveUsername)
                        .withPassword(effectivePassword)
                        .withDatabaseName(effectiveDbName)
                        .withReuse(containerConfig.isReuse());
                Labels.addDataSourceLabel(container, datasourceName);
                Volumes.addVolumes(container, containerConfig.getVolumes());

                container.withEnv(containerConfig.getContainerEnv());

                if (containerConfig.getContainerProperties().containsKey(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME)) {
                    container.withConfigurationOverride(
                            containerConfig.getContainerProperties().get(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME));
                }

                containerConfig.getAdditionalJdbcUrlProperties().forEach(container::withUrlParam);
                containerConfig.getCommand().ifPresent(container::setCommand);
                containerConfig.getInitScriptPath().ifPresent(container::withInitScripts);
                if (containerConfig.isShowLogs()) {
                    container.withLogConsumer(new JBossLoggingConsumer(LOG));
                }

                return container;
            }

            @Override
            public Optional<DevServicesDatasourceProvider.RunningDevServicesDatasource> findRunningComposeDatasource(
                    LaunchMode launchMode, boolean useSharedNetwork, DevServicesDatasourceContainerConfig containerConfig,
                    DevServicesComposeProjectBuildItem composeProjectBuildItem) {

                List<String> images = List.of(
                        containerConfig.getImageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mariadb")),
                        "maria");
                return ComposeLocator.locateContainer(composeProjectBuildItem, images, PORT, launchMode, useSharedNetwork)
                        .map(containerAddress -> configurator.composeRunningService(containerAddress, containerConfig));
            }
        });
    }

    private static class QuarkusMariaDBContainer extends MariaDBContainer implements DatasourceStartable {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusMariaDBContainer(Optional<String> imageName, OptionalInt fixedExposedPort,
                String defaultNetworkId,
                boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mariadb")))
                    .asCompatibleSubstituteFor(DockerImageName.parse(MariaDBContainer.NAME)));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "mariadb");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), PORT);
            } else {
                addExposedPort(PORT);
            }
        }

        // this is meant to be called by Quarkus code and is needed in order to not disrupt testcontainers
        // from being able to determine the status of the container (which it does by trying to acquire a connection)
        public String getEffectiveJdbcUrl() {
            if (useSharedNetwork) {
                String additionalUrlParams = constructUrlParameters("?", "&");
                return "jdbc:mariadb://" + hostName + ":" + PORT + "/" + getDatabaseName() + additionalUrlParams;
            } else {
                return super.getJdbcUrl();
            }
        }

        public String getReactiveUrl() {
            return getEffectiveJdbcUrl().replaceFirst("jdbc:mariadb:", "vertx-reactive:mysql:");
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        public String getConnectionInfo() {
            return getEffectiveJdbcUrl();
        }
    }
}
