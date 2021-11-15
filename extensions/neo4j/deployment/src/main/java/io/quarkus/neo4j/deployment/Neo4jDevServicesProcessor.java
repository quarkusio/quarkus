package io.quarkus.neo4j.deployment;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

class Neo4jDevServicesProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.neo4j.deployment");

    private static final String NEO4J_URI = "quarkus.neo4j.uri";
    private static final String NEO4J_USER_PROP = "quarkus.neo4j.authentication.username";
    private static final String NEO4J_PASSWORD_PROP = "quarkus.neo4j.authentication.password";

    static volatile Closeable closeable;
    static volatile Neo4jDevServiceConfig runningConfiguration;
    static volatile boolean first = true;

    static final class IsDockerWorking implements BooleanSupplier {

        private final io.quarkus.deployment.IsDockerWorking delegate = new io.quarkus.deployment.IsDockerWorking(true);

        @Override
        public boolean getAsBoolean() {

            return delegate.getAsBoolean();
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { IsDockerWorking.class, GlobalDevServicesConfig.Enabled.class })
    public Neo4jDevServiceBuildItem startNeo4jDevService(
            LaunchModeBuildItem launchMode,
            Neo4jBuildTimeConfig neo4jBuildTimeConfig,
            BuildProducer<DevServicesConfigResultBuildItem> devServicePropertiesProducer,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig globalDevServicesConfig) {

        var configuration = new Neo4jDevServiceConfig(neo4jBuildTimeConfig.devservices);

        if (closeable != null) {
            if (configuration.equals(runningConfiguration)) {
                return null;
            }
            shutdownNeo4j();
            runningConfiguration = null;
        }

        var compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Neo4j Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            var neo4jContainer = startNeo4j(configuration, globalDevServicesConfig.timeout);
            if (neo4jContainer != null) {
                devServicePropertiesProducer.produce(
                        new DevServicesConfigResultBuildItem(NEO4J_URI, neo4jContainer.getBoltUrl()));
                devServicePropertiesProducer.produce(new DevServicesConfigResultBuildItem(NEO4J_USER_PROP, "neo4j"));
                devServicePropertiesProducer.produce(new DevServicesConfigResultBuildItem(NEO4J_PASSWORD_PROP,
                        neo4jContainer.getAdminPassword()));

                log.infof("Dev Services started a Neo4j container reachable at %s.", neo4jContainer.getBoltUrl());
                log.infof("Neo4j Browser is reachable at %s.", neo4jContainer.getBrowserUrl());
                log.infof("The username for both endpoints is `%s`, authenticated by `%s`", "neo4j",
                        neo4jContainer.getAdminPassword());
                log.infof("Connect via Cypher-Shell: cypher-shell -u %s -p %s -a %s", "neo4j",
                        neo4jContainer.getAdminPassword(), neo4jContainer.getBoltUrl());
                closeable = neo4jContainer::close;
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (closeable != null) {
                    shutdownNeo4j();
                    log.info("Dev Services for Neo4j shut down.");
                }
                first = true;
                closeable = null;
                runningConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        runningConfiguration = configuration;

        return new Neo4jDevServiceBuildItem();
    }

    private ExtNeo4jContainer startNeo4j(Neo4jDevServiceConfig configuration, Optional<Duration> timeout) {

        if (!configuration.devServicesEnabled) {
            log.debug("Not starting Dev Services for Neo4j, as it has been disabled in the config.");
            return null;
        }

        // Check if Neo4j URL or password is set to explicitly
        if (ConfigUtils.isPropertyPresent(NEO4J_URI) || ConfigUtils.isPropertyPresent(NEO4J_USER_PROP)
                || ConfigUtils.isPropertyPresent(NEO4J_PASSWORD_PROP)) {
            log.debug("Not starting Dev Services for Neo4j, as there is explicit configuration present.");
            return null;
        }

        var boldIsReachable = Boolean.getBoolean("io.quarkus.neo4j.deployment.devservices.assumeBoltIsReachable")
                || new BoltHandshaker("localhost", 7687).isBoltPortReachable(Duration.ofSeconds(5));
        if (boldIsReachable) {
            log.warn(
                    "Not starting Dev Services for Neo4j, as the default config points to a reachable address. Be aware that your local database will be used.");
            return null;
        }

        var neo4jContainer = ExtNeo4jContainer.of(configuration);
        configuration.additionalEnv.forEach(neo4jContainer::addEnv);
        timeout.ifPresent(neo4jContainer::withStartupTimeout);
        neo4jContainer.start();
        return neo4jContainer;
    }

    private void shutdownNeo4j() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                log.error("Failed to stop Neo4j container", e);
            } finally {
                closeable = null;
            }
        }
    }

    private final static class ExtNeo4jContainer extends Neo4jContainer<ExtNeo4jContainer> {

        private static final int DEFAULT_HTTP_PORT = 7474;
        private static final int DEFAULT_BOLT_PORT = 7687;

        static ExtNeo4jContainer of(Neo4jDevServiceConfig config) {

            var container = new ExtNeo4jContainer(DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("neo4j"));
            config.fixedBoltPort.ifPresent(port -> {
                container.addFixedExposedPort(port, DEFAULT_BOLT_PORT);
            });
            config.fixedHttpPort.ifPresent(port -> container.addFixedExposedPort(port, DEFAULT_HTTP_PORT));

            var extensionScript = "/neo4j_dev_services_ext.sh";
            return container
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("/io/quarkus/neo4j/deployment" + extensionScript, 0777),
                            extensionScript)
                    .withEnv("EXTENSION_SCRIPT", extensionScript);
        }

        ExtNeo4jContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }

        String getBrowserUrl() {
            return String.format("%s/browser?dbms=bolt://%s@%s:%d", getHttpUrl(), "neo4j", getHost(),
                    getMappedPort(DEFAULT_BOLT_PORT));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);

            if (reused) {
                return;
            }

            var mappedPort = getMappedPort(DEFAULT_BOLT_PORT);
            var command = String.format("export NEO4J_dbms_connector_bolt_advertised__address=bolt://%s:%d\n", getHost(),
                    mappedPort);
            copyFileToContainer(Transferable.of(command.getBytes(StandardCharsets.UTF_8)), "/testcontainers_env");
        }
    }

    private static final class Neo4jDevServiceConfig {
        final boolean devServicesEnabled;
        final String imageName;
        final Map<String, String> additionalEnv;
        final OptionalInt fixedBoltPort;
        final OptionalInt fixedHttpPort;

        Neo4jDevServiceConfig(DevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled.orElse(true);
            this.imageName = devServicesConfig.imageName;
            this.additionalEnv = new HashMap<>(devServicesConfig.additionalEnv);
            this.fixedBoltPort = devServicesConfig.boltPort;
            this.fixedHttpPort = devServicesConfig.httpPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Neo4jDevServiceConfig that = (Neo4jDevServiceConfig) o;
            return devServicesEnabled == that.devServicesEnabled && imageName.equals(that.imageName)
                    && additionalEnv.equals(
                            that.additionalEnv)
                    && fixedBoltPort.equals(that.fixedBoltPort) && fixedHttpPort.equals(
                            that.fixedHttpPort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, additionalEnv, fixedBoltPort, fixedHttpPort);
        }
    }
}
