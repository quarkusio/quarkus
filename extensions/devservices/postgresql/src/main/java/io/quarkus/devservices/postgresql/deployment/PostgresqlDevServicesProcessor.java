package io.quarkus.devservices.postgresql.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.Volumes;
import io.quarkus.runtime.LaunchMode;

public class PostgresqlDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(PostgresqlDevServicesProcessor.class);

    @BuildStep
    ConsoleCommandBuildItem psqlCommand(DevServicesLauncherConfigResultBuildItem devServices) {
        return new ConsoleCommandBuildItem(new PostgresCommand(devServices));
    }

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupPostgres(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.POSTGRESQL, new DevServicesDatasourceProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, Optional<Duration> startupTimeout) {
                QuarkusPostgreSQLContainer container = new QuarkusPostgreSQLContainer(containerConfig.getImageName(),
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

                containerConfig.getAdditionalJdbcUrlProperties().forEach(container::withUrlParam);
                containerConfig.getCommand().ifPresent(container::setCommand);
                containerConfig.getInitScriptPath().ifPresent(container::withInitScript);

                container.start();

                LOG.info("Dev Services for PostgreSQL started.");

                return new RunningDevServicesDatasource(container.getContainerId(),
                        container.getEffectiveJdbcUrl(),
                        container.getReactiveUrl(),
                        container.getUsername(),
                        container.getPassword(),
                        new ContainerShutdownCloseable(container, "PostgreSQL"));
            }
        });
    }

    private static class QuarkusPostgreSQLContainer extends PostgreSQLContainer {

        private static final String READY_REGEX = ".*database system is ready to accept connections.*\\s";
        private static final String SKIPPING_INITIALIZATION_REGEX = ".*PostgreSQL Database directory appears to contain a database; Skipping initialization:*\\s";

        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusPostgreSQLContainer(Optional<String> imageName, OptionalInt fixedExposedPort, boolean useSharedNetwork) {
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
            this.waitStrategy = new LogMessageWaitStrategy()
                    .withRegEx("(" + READY_REGEX + ")?(" + SKIPPING_INITIALIZATION_REGEX + ")?")
                    .withTimes(2)
                    .withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));
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
