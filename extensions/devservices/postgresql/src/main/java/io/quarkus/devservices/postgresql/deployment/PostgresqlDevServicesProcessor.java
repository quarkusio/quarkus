package io.quarkus.devservices.postgresql.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.Volumes;
import io.quarkus.runtime.LaunchMode;

public class PostgresqlDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(PostgresqlDevServicesProcessor.class);

    private static final String MAX_PREPARED_TRANSACTIONS = "max_prepared_transactions";
    private static final String DEFAULT_MAX_PREPARED_TRANSACTIONS = "--" + MAX_PREPARED_TRANSACTIONS + "=100";

    private static final PostgresDatasourceServiceConfigurator configurator = new PostgresDatasourceServiceConfigurator();

    @BuildStep
    ConsoleCommandBuildItem psqlCommand(DevServicesLauncherConfigResultBuildItem devServices) {
        return new ConsoleCommandBuildItem(new PostgresCommand(devServices));
    }

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupPostgres(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DevServicesConfig devServicesConfig) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.POSTGRESQL, new DevServicesDatasourceProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, Optional<Duration> startupTimeout) {

                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);

                String effectiveUsername = containerConfig.getUsername().orElse(username.orElse(DEFAULT_DATABASE_USERNAME));
                String effectivePassword = containerConfig.getPassword().orElse(password.orElse(DEFAULT_DATABASE_PASSWORD));
                String effectiveDbName = containerConfig.getDbName().orElse(
                        DataSourceUtil.isDefault(datasourceName) ? DEFAULT_DATABASE_NAME : datasourceName);

                Supplier<RunningDevServicesDatasource> createDevService = () -> {
                    QuarkusPostgreSQLContainer container = new QuarkusPostgreSQLContainer(containerConfig.getImageName(),
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

                    containerConfig.getAdditionalJdbcUrlProperties().forEach(container::withUrlParam);
                    String augmentedCommand;
                    if (containerConfig.getCommand().isPresent()) {
                        String originalCommand = containerConfig.getCommand().get();
                        augmentedCommand = originalCommand.contains(MAX_PREPARED_TRANSACTIONS) ? originalCommand
                                : originalCommand + " " + DEFAULT_MAX_PREPARED_TRANSACTIONS;
                    } else {
                        augmentedCommand = DEFAULT_MAX_PREPARED_TRANSACTIONS;
                    }
                    container.setCommand(augmentedCommand);

                    containerConfig.getInitScriptPath().ifPresent(container::withInitScripts);
                    if (containerConfig.isShowLogs()) {
                        container.withLogConsumer(new JBossLoggingConsumer(LOG));
                    }

                    container.start();

                    LOG.info("Dev Services for PostgreSQL started.");

                    return new RunningDevServicesDatasource(container.getContainerId(),
                            container.getEffectiveJdbcUrl(),
                            container.getReactiveUrl(),
                            container.getUsername(),
                            container.getPassword(),
                            new ContainerShutdownCloseable(container, "PostgreSQL"));
                };
                List<String> images = List.of(
                        containerConfig.getImageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("postgresql")),
                        "postgres");
                return ComposeLocator
                        .locateContainer(composeProjectBuildItem, images, POSTGRESQL_PORT, launchMode, useSharedNetwork)
                        .map(containerAddress -> configurator.composeRunningService(containerAddress, containerConfig))
                        .orElseGet(createDevService);
            }
        });
    }

    private static class QuarkusPostgreSQLContainer extends PostgreSQLContainer {

        private static final String READY_REGEX = ".*database system is ready to accept connections.*\\s";
        private static final String SKIPPING_INITIALIZATION_REGEX = ".*PostgreSQL Database directory appears to contain a database; Skipping initialization:*\\s";

        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusPostgreSQLContainer(Optional<String> imageName, OptionalInt fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("postgresql")))
                    .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE)));

            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            // Workaround for https://github.com/testcontainers/testcontainers-java/issues/4799.
            // The motivation of this custom wait strategy is that Testcontainers fails to start a Postgresql database when it
            // has been already initialized.
            // This custom wait strategy will work fine regardless of the state of the Postgresql database.
            // More information in the issue ticket in Testcontainers.

            // Added Wait.forListeningPort() for https://github.com/quarkusio/quarkus/issues/25682
            // as suggested by https://github.com/testcontainers/testcontainers-java/pull/6309
            this.waitStrategy = new WaitAllStrategy()
                    .withStrategy(Wait.forLogMessage("(" + READY_REGEX + ")?(" + SKIPPING_INITIALIZATION_REGEX + ")?", 2))
                    .withStrategy(Wait.forListeningPort())
                    .withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "postgres");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), POSTGRESQL_PORT);
            } else {
                addExposedPort(POSTGRESQL_PORT);
            }
        }

        // this is meant to be called by Quarkus code and is not strictly needed
        // in the PostgreSQL case as testcontainers does not try to establish
        // a connection to determine if the container is ready, but we do it anyway to be consistent across
        // DB containers
        public String getEffectiveJdbcUrl() {
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

        public String getReactiveUrl() {
            return getEffectiveJdbcUrl().replaceFirst("jdbc:", "vertx-reactive:");
        }
    }
}
