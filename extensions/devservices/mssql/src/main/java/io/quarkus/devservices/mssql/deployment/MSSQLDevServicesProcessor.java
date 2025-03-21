package io.quarkus.devservices.mssql.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_STRONG_PASSWORD;
import static org.testcontainers.containers.MSSQLServerContainer.MS_SQL_SERVER_PORT;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.Volumes;
import io.quarkus.runtime.LaunchMode;

public class MSSQLDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(MSSQLDevServicesProcessor.class);

    /**
     * Using SA doesn't work with all collations so let's use the lowercase version instead.
     */
    private static final String DEFAULT_USERNAME = "sa";

    private static final MSSQLDatasourceServiceConfigurator configurator = new MSSQLDatasourceServiceConfigurator();

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupMSSQL(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DevServicesConfig devServicesConfig) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.MSSQL, new DevServicesDatasourceProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, Optional<Duration> startupTimeout) {

                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);

                Supplier<RunningDevServicesDatasource> startService = () -> {
                    QuarkusMSSQLServerContainer container = new QuarkusMSSQLServerContainer(containerConfig.getImageName(),
                            containerConfig.getFixedExposedPort(),
                            composeProjectBuildItem.getDefaultNetworkId(),
                            !devServicesSharedNetworkBuildItem.isEmpty());
                    startupTimeout.ifPresent(container::withStartupTimeout);

                    String effectivePassword = containerConfig.getPassword()
                            .orElse(password.orElse(DEFAULT_DATABASE_STRONG_PASSWORD));

                    // Defining the database name and the username is not supported by this container yet
                    container.withPassword(effectivePassword)
                            .withReuse(containerConfig.isReuse());
                    Labels.addDataSourceLabel(container, datasourceName);
                    Volumes.addVolumes(container, containerConfig.getVolumes());

                    container.withEnv(containerConfig.getContainerEnv());

                    containerConfig.getAdditionalJdbcUrlProperties().forEach(container::withUrlParam);
                    containerConfig.getCommand().ifPresent(container::setCommand);
                    containerConfig.getInitScriptPath().ifPresent(container::withInitScripts);
                    if (containerConfig.isShowLogs()) {
                        container.withLogConsumer(new JBossLoggingConsumer(LOG));
                    }

                    container.start();

                    LOG.info("Dev Services for Microsoft SQL Server started.");

                    return new RunningDevServicesDatasource(container.getContainerId(),
                            container.getEffectiveJdbcUrl(),
                            container.getReactiveUrl(),
                            DEFAULT_USERNAME,
                            container.getPassword(),
                            new ContainerShutdownCloseable(container, "Microsoft SQL Server"));
                };
                List<String> images = List.of(
                        containerConfig.getImageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mssql")),
                        "mssql");
                return ComposeLocator
                        .locateContainer(composeProjectBuildItem, images, MS_SQL_SERVER_PORT, launchMode, useSharedNetwork)
                        .map(containerAddress -> configurator.composeRunningService(containerAddress, containerConfig))
                        .orElseGet(startService);
            }
        });
    }

    private static class QuarkusMSSQLServerContainer extends MSSQLServerContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusMSSQLServerContainer(Optional<String> imageName, OptionalInt fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mssql")))
                    .asCompatibleSubstituteFor(MSSQLServerContainer.IMAGE));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "mssql");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), MS_SQL_SERVER_PORT);
            } else {
                addExposedPort(MS_SQL_SERVER_PORT);
            }
        }

        // this is meant to be called by Quarkus code and is needed in order to not disrupt testcontainers
        // from being able to determine the status of the container (which it does by trying to acquire a connection)
        public String getEffectiveJdbcUrl() {
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

        public String getReactiveUrl() {
            StringBuilder url = new StringBuilder("vertx-reactive:sqlserver://");
            if (useSharedNetwork) {
                url.append(hostName).append(":").append(MS_SQL_SERVER_PORT);
            } else {
                url.append(this.getHost()).append(":").append(this.getMappedPort(MS_SQL_SERVER_PORT));
            }
            return url.toString();
        }
    }
}
