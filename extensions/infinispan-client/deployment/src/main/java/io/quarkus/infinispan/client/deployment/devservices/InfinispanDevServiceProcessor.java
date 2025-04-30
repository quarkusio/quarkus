package io.quarkus.infinispan.client.deployment.devservices;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;
import static org.infinispan.server.test.core.InfinispanContainer.DEFAULT_USERNAME;
import static org.infinispan.server.test.core.InfinispanContainer.IMAGE_BASENAME;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.util.Version;
import org.infinispan.server.test.core.InfinispanContainer;
import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.infinispan.client.runtime.InfinispanClientBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.infinispan.client.runtime.InfinispanClientsBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanDevServicesConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class InfinispanDevServiceProcessor {
    private static final Logger log = Logger.getLogger(InfinispanDevServiceProcessor.class);

    /**
     * Label to add to shared Dev Service for Infinispan running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-infinispan";
    public static final int DEFAULT_INFINISPAN_PORT = ConfigurationProperties.DEFAULT_HOTROD_PORT;
    private static final ContainerLocator infinispanContainerLocator = locateContainerWithLabels(DEFAULT_INFINISPAN_PORT,
            DEV_SERVICE_LABEL);

    private static final String DEFAULT_PASSWORD = "password";
    private static final String QUARKUS = "quarkus.";
    private static final String DOT = ".";
    private static final String UNDERSCORE = "_";
    private static volatile Map<String, RunningDevService> devServices;
    private static volatile Map<String, InfinispanClientBuildTimeConfig.DevServiceConfiguration> capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private static volatile Map<String, String> properties = new HashMap<>();

    @BuildStep
    public List<DevServicesResultBuildItem> startInfinispanContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            InfinispanClientsBuildTimeConfig config,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        // figure out if we need to shut down and restart existing Infinispan containers
        // if not and the Infinispan containers have already started we just return
        if (devServices != null) {
            boolean restartRequired = false;
            for (String devServiceName : devServices.keySet()) {
                InfinispanClientBuildTimeConfig.DevServiceConfiguration devServiceConfig = capturedDevServicesConfiguration.get(
                        devServiceName);
                restartRequired = restartRequired
                        || !config.getInfinispanClientBuildTimeConfig(devServiceName).devservices().equals(
                                devServiceConfig);

            }

            if (!restartRequired) {
                return devServices.values().stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
            }

            for (Closeable closeable : devServices.values()) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    log.error("Failed to stop infinispan container", e);
                }
            }
            devServices = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = new HashMap<>();
        Map<String, RunningDevService> newDevServices = new HashMap<>();
        capturedDevServicesConfiguration.put(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME,
                config.defaultInfinispanClient().devservices());
        for (Map.Entry<String, InfinispanClientBuildTimeConfig> entry : config.namedInfinispanClients().entrySet()) {
            capturedDevServicesConfiguration.put(entry.getKey(), entry.getValue().devservices());
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Infinispan Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);

        runInfinispanDevService(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME, launchMode,
                compressor, dockerStatusBuildItem, composeProjectBuildItem,
                devServicesSharedNetworkBuildItem, config.defaultInfinispanClient(),
                devServicesConfig, newDevServices,
                properties);

        config.namedInfinispanClients().entrySet().forEach(dServ -> {
            runInfinispanDevService(dServ.getKey(), launchMode,
                    compressor, dockerStatusBuildItem, composeProjectBuildItem,
                    devServicesSharedNetworkBuildItem, dServ.getValue(),
                    devServicesConfig,
                    newDevServices, properties);
        });

        devServices = newDevServices;

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devServices != null) {
                    for (Closeable closeable : devServices.values()) {
                        try {
                            closeable.close();
                        } catch (Throwable t) {
                            log.error("Failed to stop infinispan", t);
                        }
                    }
                }
                first = true;
                devServices = null;
                capturedDevServicesConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        return devServices.values().stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
    }

    private void runInfinispanDevService(String clientName,
            LaunchModeBuildItem launchMode,
            StartupLogCompressor compressor,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            InfinispanClientBuildTimeConfig config,
            DevServicesConfig devServicesConfig,
            Map<String, RunningDevService> newDevServices,
            Map<String, String> properties) {
        try {

            InfinispanDevServicesConfig namedDevServiceConfig = config.devservices().devservices();
            RunningDevService devService = startContainer(clientName, dockerStatusBuildItem, composeProjectBuildItem,
                    namedDevServiceConfig,
                    launchMode.getLaunchMode(),
                    !devServicesSharedNetworkBuildItem.isEmpty(), devServicesConfig.timeout(), properties);
            if (devService == null) {
                compressor.closeAndDumpCaptured();
                return;
            }
            newDevServices.put(clientName, devService);
            log.infof("The infinispan server is ready to accept connections on %s",
                    devService.getConfig().get(getConfigPrefix(clientName) + "hosts"));
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }

    private RunningDevService startContainer(String clientName, DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            InfinispanDevServicesConfig devServicesConfig,
            LaunchMode launchMode,
            boolean useSharedNetwork,
            Optional<Duration> timeout,
            Map<String, String> properties) {
        if (!devServicesConfig.enabled()) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Infinispan as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(clientName);

        boolean needToStart = !ConfigUtils.isPropertyNonEmpty(configPrefix + "hosts")
                && !ConfigUtils.isPropertyNonEmpty(configPrefix + "server-list");

        if (!needToStart) {
            log.debug("Not starting Dev Services for Infinispan as 'hosts', 'uri' or 'server-list' have been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "Please configure 'quarkus.infinispan-client.hosts' or 'quarkus.infinispan-client.uri' or get a working Docker instance");
            return null;
        }
        log.infof("Starting Dev Services for connection %s", clientName);
        log.infof("Applying Dev Services config %s", devServicesConfig);

        Supplier<RunningDevService> infinispanServerSupplier = () -> {
            QuarkusInfinispanContainer infinispanContainer = new QuarkusInfinispanContainer(clientName, devServicesConfig,
                    launchMode,
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork);
            timeout.ifPresent(infinispanContainer::withStartupTimeout);
            infinispanContainer.withEnv(devServicesConfig.containerEnv());
            infinispanContainer.start();

            return getRunningDevService(clientName, infinispanContainer.getContainerId(), infinispanContainer::close,
                    infinispanContainer.getHost() + ":" + infinispanContainer.getPort(),
                    infinispanContainer.getUser(), infinispanContainer.getPassword(), properties);
        };

        return infinispanContainerLocator
                .locateContainer(devServicesConfig.serviceName(), devServicesConfig.shared(), launchMode)
                .map(containerAddress -> getRunningDevService(clientName, containerAddress.getId(), null,
                        containerAddress.getUrl(), DEFAULT_USERNAME, DEFAULT_PASSWORD, properties)) // TODO can this be always right ?
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(devServicesConfig.imageName().orElse(IMAGE_BASENAME), "infinispan"),
                        DEFAULT_INFINISPAN_PORT, launchMode, useSharedNetwork)
                        .map(address -> getRunningDevService(clientName, address, properties)))
                .orElseGet(infinispanServerSupplier);
    }

    private RunningDevService getRunningDevService(String clientName, ContainerAddress address, Map<String, String> config) {
        RunningContainer container = address.getRunningContainer();
        if (container == null) {
            return null;
        }
        return getRunningDevService(clientName, address.getId(), null, address.getUrl(),
                container.tryGetEnv("USER").orElse(DEFAULT_USERNAME),
                container.tryGetEnv("PASS").orElse(DEFAULT_PASSWORD), config);
    }

    private RunningDevService getRunningDevService(String clientName, String containerId, Closeable closeable, String hosts,
            String username, String password, Map<String, String> config) {
        config.put(getConfigPrefix(clientName) + "hosts", hosts);
        config.put(getConfigPrefix(clientName) + "client-intelligence", ClientIntelligence.BASIC.name());
        config.put(getConfigPrefix(clientName) + "username", username);
        config.put(getConfigPrefix(clientName) + "password", password);
        return new RunningDevService(runningServiceName(clientName), containerId, closeable, config);
    }

    private String runningServiceName(String clientName) {
        if (InfinispanClientUtil.isDefault(clientName)) {
            return Feature.INFINISPAN_CLIENT.getName();
        }

        return Feature.INFINISPAN_CLIENT.getName() + UNDERSCORE + clientName;

    }

    private String getConfigPrefix(String name) {
        if (name.equals(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME)) {
            return InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_MAPPING_PREFIX + DOT;
        }

        return InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_MAPPING_PREFIX + DOT + name + DOT;
    }

    private static class QuarkusInfinispanContainer extends InfinispanContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusInfinispanContainer(String clientName, InfinispanDevServicesConfig config,
                LaunchMode launchMode, String defaultNetworkId, boolean useSharedNetwork) {
            super(config.imageName().orElse(IMAGE_BASENAME + ":" + Version.getUnbrandedVersion()));
            this.fixedExposedPort = config.port();
            this.useSharedNetwork = useSharedNetwork;
            if (launchMode == DEVELOPMENT) {
                String label = config.serviceName();
                if (InfinispanClientUtil.DEFAULT_INFINISPAN_DEV_SERVICE_NAME.equals(label)
                        && !InfinispanClientUtil.isDefault(clientName)) {
                    // Adds the client name suffix to create a different service name in named connections
                    label = label + UNDERSCORE + clientName;
                }
                withLabel(DEV_SERVICE_LABEL, label);
            }
            withUser(DEFAULT_USERNAME);
            withPassword(InfinispanDevServiceProcessor.DEFAULT_PASSWORD);
            String command = "-c infinispan.xml";
            if (config.site().isPresent()) {
                command = "-c infinispan-xsite.xml -Dinfinispan.site.name=" + config.site().get();
            }
            command = command + config.configFiles().map(files -> files.stream().map(file -> {
                String userConfigFile = "/user-config/" + file;
                withClasspathResourceMapping(file, userConfigFile, BindMode.READ_ONLY);
                return " -c " + userConfigFile;
            }).collect(Collectors.joining())).orElse("");

            if (config.tracing().orElse(false)) {
                log.warn(
                        "Starting with Infinispan 15.0, Infinispan support for instrumentation of the server via OpenTelemetry has evolved. Enabling tracing by setting `quarkus.infinispan-client.devservices.tracing.enabled=true` doesn't work anymore.\n"
                                +
                                "You need to use the `quarkus.infinispan-client.devservices.tracing.enabled` property and provide a JSON, XML or YAML file as follows. Check https://quarkus.io/guides/infinispan-dev-services for more information");
                log.warn("infinispan:\n" +
                        "        cacheContainer:\n" +
                        "                tracing:\n" +
                        "                        collector-endpoint: \"http://jaeger:4318\"\n" +
                        "                        enabled: true\n" +
                        "                        exporter-protocol: \"OTLP\"\n" +
                        "                        service-name: \"infinispan-server\"\n" +
                        "                        security: false");
            }

            if (config.mcastPort().isPresent()) {
                command = command + " -Djgroups.mcast_port=" + config.mcastPort().getAsInt();
            }

            config.artifacts().ifPresent(a -> withArtifacts(a.toArray(new String[0])));

            withCommand(command);
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "infinispan");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), DEFAULT_INFINISPAN_PORT);
            } else {
                addExposedPort(DEFAULT_INFINISPAN_PORT);
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return DEFAULT_INFINISPAN_PORT;
            }

            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return super.getFirstMappedPort();
        }

        public String getUser() {
            return DEFAULT_USERNAME;
        }

        public String getPassword() {
            return InfinispanDevServiceProcessor.DEFAULT_PASSWORD;
        }

        @Override
        public String getHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }
    }
}
