package io.quarkus.devservices.db2.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;
import static org.testcontainers.db2.Db2Container.DB2_PORT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.db2.Db2Container;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.Volumes;
import io.quarkus.runtime.LaunchMode;

public class DB2DevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(DB2DevServicesProcessor.class);

    private static final DB2DatasourceServiceConfigurator configurator = new DB2DatasourceServiceConfigurator();

    /**
     * Generates a short hash (4 hex characters) from the input string using SHA-224.
     * Used to create unique but short database names for DB2's 8-character limit.
     */
    private static String generateShortHash(String originalName) {
        try {
            final byte[] nameAsBytes = originalName.getBytes(StandardCharsets.UTF_8);
            MessageDigest messageDigest224 = MessageDigest.getInstance("SHA-224");
            byte[] hashedByteArray = messageDigest224.digest(nameAsBytes);

            // Encode first 2 bytes as hex (4 chars) - using hex instead of Base64
            // because DB2 database names must be alphanumeric only
            StringBuilder hexResult = new StringBuilder();
            for (int i = 0; i < 2; i++) {
                hexResult.append(String.format("%02x", hashedByteArray[i] & 0xFF));
            }
            return hexResult.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-224 is always available in Java
            throw new RuntimeException(e);
        }
    }

    @BuildStep
    DevServicesDatasourceProviderBuildItem setupDB2(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DevServicesConfig devServicesConfig) {
        return new DevServicesDatasourceProviderBuildItem(DatabaseKind.DB2, new DevServicesDatasourceProvider() {
            @Override
            public RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
                    String datasourceName, DevServicesDatasourceContainerConfig containerConfig,
                    LaunchMode launchMode, Optional<Duration> startupTimeout) {

                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);
                String effectiveUsername = containerConfig.getUsername().orElse(username.orElse(DEFAULT_DATABASE_USERNAME));
                String effectivePassword = containerConfig.getPassword().orElse(password.orElse(DEFAULT_DATABASE_PASSWORD));
                String effectiveDbName = containerConfig.getDbName().orElseGet(() -> {
                    if (DataSourceUtil.isDefault(datasourceName)) {
                        return DEFAULT_DATABASE_NAME;
                    }
                    // DB2 has an 8-character limit for database names
                    // See: https://github.com/quarkusio/quarkus/issues/51225
                    if (datasourceName.length() > 8) {
                        // Use prefix (4 chars) + SHA-224 hash (4 chars) to avoid collisions
                        String prefix = datasourceName.substring(0, 4);
                        String hash = generateShortHash(datasourceName);
                        String dbName = prefix + hash;
                        LOG.warnf("DB2 database name '%s' exceeds 8 character limit. Using '%s' for dev services instead. " +
                                "Set 'quarkus.datasource.\"%s\".devservices.db-name' to specify a custom database name.",
                                datasourceName, dbName, datasourceName);
                        return dbName;
                    }
                    return datasourceName;
                });

                Supplier<RunningDevServicesDatasource> maybe = () -> {
                    // We just use this to enable a workaround, so:
                    boolean podman = ContainerRuntimeUtil.detectContainerRuntime(
                            false, // Don't fail if container runtime can't be found.
                            true // Be silent.
                    ).isPodman();
                    QuarkusDb2Container container = new QuarkusDb2Container(containerConfig.getImageName(),
                            containerConfig.getFixedExposedPort(), composeProjectBuildItem.getDefaultNetworkId(),
                            useSharedNetwork, podman);
                    startupTimeout.ifPresent(container::withStartupTimeout);

                    container.withUsername(effectiveUsername)
                            .withPassword(effectivePassword)
                            .withDatabaseName(effectiveDbName)
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

                    LOG.info("Dev Services for IBM Db2 started.");

                    return new RunningDevServicesDatasource(container.getContainerId(),
                            container.getEffectiveJdbcUrl(),
                            container.getReactiveUrl(),
                            container.getUsername(),
                            container.getPassword(),
                            new ContainerShutdownCloseable(container, "IBM Db2"));
                };
                List<String> images = List.of(containerConfig.getImageName()
                        .orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("db2")),
                        "db2");
                return ComposeLocator.locateContainer(composeProjectBuildItem, images, DB2_PORT, launchMode, useSharedNetwork)
                        .map(containerAddress -> configurator.composeRunningService(containerAddress, containerConfig))
                        .orElseGet(maybe);
            }
        });
    }

    private static class QuarkusDb2Container extends Db2Container {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;
        private final boolean podman;

        private final String hostName;

        public QuarkusDb2Container(Optional<String> imageName, OptionalInt fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork, boolean podman) {
            super(DockerImageName.parse(imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("db2")))
                    .asCompatibleSubstituteFor(DockerImageName.parse("icr.io/db2_community/db2")));
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.podman = podman;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "db2");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), DB2_PORT);
            } else {
                addExposedPorts(DB2_PORT);
            }

            // Workaround for DB2 not starting on Podman
            // See https://github.com/quarkusio/quarkus/issues/33104#issuecomment-3131453891
            // Since this probably leads to DB2 not storing data where it should be stored,
            // we only use this when there are no mounts.
            // When using mounts, developers might still face https://github.com/quarkusio/quarkus/issues/33104
            // -- but then again, maybe not, considering reports on that issue that explicit mounts fix the issue.
            if (podman && !getEnvMap().containsKey("PERSISTENT_HOME") && getBinds().isEmpty()) {
                addEnv("PERSISTENT_HOME", "false");
            }
        }

        // this is meant to be called by Quarkus code and is not strictly needed
        // in the DB2 case as testcontainers does not try to establish
        // a connection to determine if the container is ready, but we do it anyway to be consistent across
        // DB containers
        public String getEffectiveJdbcUrl() {
            if (useSharedNetwork) {
                // in this case we expose the URL using the network alias we created in 'configure'
                // and the container port since the application communicating with this container
                // won't be doing port mapping
                String additionalUrlParams = constructUrlParameters(":", ";", ";");
                return "jdbc:db2://" + hostName + ":" + DB2_PORT + "/" + getDatabaseName() + additionalUrlParams;
            } else {
                return super.getJdbcUrl();
            }
        }

        public String getReactiveUrl() {
            return getEffectiveJdbcUrl().replaceFirst("jdbc:", "vertx-reactive:");
        }
    }
}
