package io.quarkus.devservices.oracle.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
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

public class OracleDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(OracleDevServicesProcessor.class);

    /**
     * This is the container name as defined by the Testcontainer's OracleContainer:
     * does not necessarily match the container name that Quarkus will default to use.
     */
    public static final String ORIGINAL_IMAGE_NAME = "gvenzl/oracle-xe";
    public static final int PORT = 1521;

    private static final OracleDatasourceServiceConfigurator configurator = new OracleDatasourceServiceConfigurator();

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupOracle(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DevServicesConfig devServicesConfig) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.ORACLE, new DevServicesDatasourceProvider() {
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

                Supplier<RunningDevServicesDatasource> startService = () -> {

                    QuarkusOracleServerContainer container = new QuarkusOracleServerContainer(containerConfig.getImageName(),
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

                    // We need to limit the maximum amount of CPUs being used by the container;
                    // otherwise the hardcoded memory configuration of the DB might not be enough to successfully boot it.
                    // See https://github.com/gvenzl/oci-oracle-xe/issues/64
                    // I choose to limit it to "2 cpus": should be more than enough for any local testing needs,
                    // and keeps things simple.
                    container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withNanoCPUs(2_000_000_000l));

                    containerConfig.getAdditionalJdbcUrlProperties().forEach(container::withUrlParam);
                    containerConfig.getCommand().ifPresent(container::setCommand);
                    containerConfig.getInitScriptPath().ifPresent(container::withInitScripts);
                    if (containerConfig.getInitPrivilegedScriptPath().isPresent()) {
                        for (String initScript : containerConfig.getInitPrivilegedScriptPath().get()) {
                            container.withCopyFileToContainer(MountableFile.forClasspathResource(initScript),
                                    "/container-entrypoint-startdb.d/" + initScript);
                        }
                    }
                    if (containerConfig.isShowLogs()) {
                        container.withLogConsumer(new JBossLoggingConsumer(LOG));
                    }

                    container.start();

                    LOG.info("Dev Services for Oracle started.");

                    return new RunningDevServicesDatasource(container.getContainerId(),
                            container.getEffectiveJdbcUrl(),
                            container.getReactiveUrl(),
                            container.getUsername(),
                            container.getPassword(),
                            new ContainerShutdownCloseable(container, "Oracle"));
                };
                List<String> images = List.of(
                        containerConfig.getImageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("oracle")),
                        "oracle");
                return ComposeLocator.locateContainer(composeProjectBuildItem, images, PORT, launchMode, useSharedNetwork)
                        .map(containerAddress -> configurator.composeRunningService(containerAddress, containerConfig))
                        .orElseGet(startService);
            }
        });
    }

    private static class QuarkusOracleServerContainer extends OracleContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusOracleServerContainer(Optional<String> imageName, OptionalInt fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(DockerImageName
                    .parse(imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("oracle")))
                    .asCompatibleSubstituteFor(OracleDevServicesProcessor.ORIGINAL_IMAGE_NAME));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "oracle");
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
                // in this case we expose the URL using the network alias we created in 'configure'
                // and the container port since the application communicating with this container
                // won't be doing port mapping
                return "jdbc:oracle:thin:" + "@" + hostName + ":" + PORT + "/" + getDatabaseName();
            } else {
                return super.getJdbcUrl();
            }
        }

        public String getReactiveUrl() {
            return getEffectiveJdbcUrl().replaceFirst("jdbc:", "vertx-reactive:");
        }
    }
}
