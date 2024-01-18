package io.quarkus.infinispan.client.deployment.devservices;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;
import static org.infinispan.server.test.core.InfinispanContainer.DEFAULT_USERNAME;

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

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.infinispan.client.runtime.InfinispanClientBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.infinispan.client.runtime.InfinispanClientsBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanDevServicesConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class InfinispanDevServiceProcessor {
    private static final Logger log = Logger.getLogger(InfinispanDevServiceProcessor.class);

    /**
     * Label to add to shared Dev Service for Infinispan running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-infinispan";
    public static final int DEFAULT_INFINISPAN_PORT = ConfigurationProperties.DEFAULT_HOTROD_PORT;
    private static final ContainerLocator infinispanContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            DEFAULT_INFINISPAN_PORT);

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
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig globalDevServicesConfig) {

        // figure out if we need to shut down and restart existing Infinispan containers
        // if not and the Infinispan containers have already started we just return
        if (devServices != null) {
            boolean restartRequired = false;
            for (String devServiceName : devServices.keySet()) {
                InfinispanClientBuildTimeConfig.DevServiceConfiguration devServiceConfig = capturedDevServicesConfiguration.get(
                        devServiceName);
                restartRequired = restartRequired
                        || !config.getInfinispanClientBuildTimeConfig(devServiceName).devService.equals(
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
                config.defaultInfinispanClient.devService);
        for (Map.Entry<String, InfinispanClientBuildTimeConfig> entry : config.namedInfinispanClients.entrySet()) {
            capturedDevServicesConfiguration.put(entry.getKey(), entry.getValue().devService);
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Infinispan Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);

        runInfinispanDevService(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME, launchMode,
                compressor, dockerStatusBuildItem, devServicesSharedNetworkBuildItem, config.defaultInfinispanClient,
                globalDevServicesConfig, newDevServices,
                properties);

        config.namedInfinispanClients.entrySet().forEach(dServ -> {
            runInfinispanDevService(dServ.getKey(), launchMode,
                    compressor, dockerStatusBuildItem, devServicesSharedNetworkBuildItem, dServ.getValue(),
                    globalDevServicesConfig,
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
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            InfinispanClientBuildTimeConfig config,
            GlobalDevServicesConfig globalDevServicesConfig,
            Map<String, RunningDevService> newDevServices,
            Map<String, String> properties) {
        try {
            log.infof("Starting Dev Service for connection %s", clientName);
            InfinispanDevServicesConfig namedDevServiceConfig = config.devService.devservices;
            log.infof("Apply Dev Services config %s", namedDevServiceConfig);
            RunningDevService devService = startContainer(clientName, dockerStatusBuildItem, namedDevServiceConfig,
                    launchMode.getLaunchMode(),
                    !devServicesSharedNetworkBuildItem.isEmpty(), globalDevServicesConfig.timeout, properties);
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
            InfinispanDevServicesConfig devServicesConfig, LaunchMode launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout, Map<String, String> properties) {
        if (!devServicesConfig.enabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Infinispan as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(clientName);
        log.info("Config prefix " + configPrefix);

        boolean needToStart = !ConfigUtils.isPropertyPresent(configPrefix + "hosts")
                && !ConfigUtils.isPropertyPresent(configPrefix + "server-list");

        if (!needToStart) {
            log.debug("Not starting Dev Services for Infinispan as 'hosts', 'uri' or 'server-list' have been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn(
                    "Please configure 'quarkus.infinispan-client.hosts' or 'quarkus.infinispan-client.uri' or get a working docker instance");
            return null;
        }

        Supplier<RunningDevService> infinispanServerSupplier = () -> {
            QuarkusInfinispanContainer infinispanContainer = new QuarkusInfinispanContainer(clientName, devServicesConfig,
                    launchMode,
                    useSharedNetwork);
            timeout.ifPresent(infinispanContainer::withStartupTimeout);
            infinispanContainer.withEnv(devServicesConfig.containerEnv);
            infinispanContainer.start();

            return getRunningDevService(clientName, infinispanContainer.getContainerId(), infinispanContainer::close,
                    infinispanContainer.getHost() + ":" + infinispanContainer.getPort(),
                    infinispanContainer.getUser(), infinispanContainer.getPassword(), properties);
        };

        return infinispanContainerLocator.locateContainer(devServicesConfig.serviceName, devServicesConfig.shared, launchMode)
                .map(containerAddress -> getRunningDevService(clientName, containerAddress.getId(), null,
                        containerAddress.getUrl(), DEFAULT_USERNAME, DEFAULT_PASSWORD, properties)) // TODO can this be always right ?
                .orElseGet(infinispanServerSupplier);
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
            return QUARKUS + InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_ROOT_NAME + DOT;
        }

        return QUARKUS + InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_ROOT_NAME + DOT + name + DOT;
    }

    private static class QuarkusInfinispanContainer extends InfinispanContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusInfinispanContainer(String clientName, InfinispanDevServicesConfig config,
                LaunchMode launchMode, boolean useSharedNetwork) {
            super(config.imageName.orElse(IMAGE_BASENAME + ":" + Version.getMajorMinor()));
            this.fixedExposedPort = config.port;
            this.useSharedNetwork = useSharedNetwork;
            if (launchMode == DEVELOPMENT) {
                String label = config.serviceName;
                if (InfinispanClientUtil.DEFAULT_INFINISPAN_DEV_SERVICE_NAME.equals(label)
                        && !InfinispanClientUtil.isDefault(clientName)) {
                    // Adds the client name suffix to create a different service name in named connections
                    label = label + UNDERSCORE + clientName;
                }
                withLabel(DEV_SERVICE_LABEL, label);
            }
            withUser(DEFAULT_USERNAME);
            withPassword(InfinispanDevServiceProcessor.DEFAULT_PASSWORD);
            String command = "";
            if (config.site.isPresent()) {
                command = "-c infinispan-xsite.xml -Dinfinispan.site.name=" + config.site.get();
            }
            if (config.mcastPort.isPresent()) {
                command = command + " -Djgroups.mcast_port=" + config.mcastPort.getAsInt();
            }
            if (config.tracing.isPresent()) {
                command = command + " -Dinfinispan.tracing.enabled=" + config.tracing.get();
                command = command + " -Dotel.exporter.otlp.endpoint=" + config.exporterOtlpEndpoint.get();
                command = command + " -Dotel.service.name=infinispan-server-service -Dotel.metrics.exporter=none";
            }
            if (!command.isEmpty()) {
                withCommand(command);
            }
            config.artifacts.ifPresent(a -> withArtifacts(a.toArray(new String[0])));
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "infinispan");
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
