package io.quarkus.infinispan.client.deployment.devservices;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;
import static org.infinispan.server.test.core.InfinispanContainer.DEFAULT_USERNAME;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.server.test.core.InfinispanContainer;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
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
import io.quarkus.infinispan.client.deployment.InfinispanClientDevServiceBuildTimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

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
    private static volatile List<RunningDevService> devServices;
    private static volatile InfinispanClientDevServiceBuildTimeConfig.DevServiceConfiguration capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class })
    public List<DevServicesResultBuildItem> startInfinispanContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            InfinispanClientDevServiceBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, GlobalDevServicesConfig devServicesConfig) {

        // figure out if we need to shut down and restart existing Infinispan containers
        // if not and the Infinispan containers have already started we just return
        if (devServices != null) {
            boolean restartRequired = !config.devService.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return devServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
            }
            for (Closeable closeable : devServices) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    log.error("Failed to stop infinispan container", e);
                }
            }
            devServices = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = config.devService;
        List<RunningDevService> newDevServices = new ArrayList<>();

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Infinispan Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService devService = startContainer(dockerStatusBuildItem, config.devService.devservices,
                    launchMode.getLaunchMode(),
                    !devServicesSharedNetworkBuildItem.isEmpty(), devServicesConfig.timeout);
            if (devService == null) {
                compressor.closeAndDumpCaptured();
                return null;
            }
            newDevServices.add(devService);
            log.infof("The infinispan server is ready to accept connections on %s",
                    devService.getConfig().get(getConfigPrefix() + "server-list"));
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        devServices = newDevServices;

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devServices != null) {
                    for (Closeable closeable : devServices) {
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
        return devServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
    }

    private RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            InfinispanDevServicesConfig devServicesConfig, LaunchMode launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!devServicesConfig.enabled) {
            // explicitly disabled
            log.debug("Not starting devservices for Infinispan as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix();

        boolean needToStart = !ConfigUtils.isPropertyPresent(configPrefix + "server-list");
        if (!needToStart) {
            log.debug("Not starting devservices for Infinispan as 'server-list' have been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Please configure 'quarkus.infinispan-client.server-list' or get a working docker instance");
            return null;
        }

        Supplier<RunningDevService> defaultInfinispanServerSupplier = () -> {
            QuarkusInfinispanContainer infinispanContainer = new QuarkusInfinispanContainer(devServicesConfig.port,
                    launchMode == DEVELOPMENT ? devServicesConfig.serviceName : null, useSharedNetwork,
                    devServicesConfig.artifacts);
            timeout.ifPresent(infinispanContainer::withStartupTimeout);
            infinispanContainer.start();

            return getRunningDevService(infinispanContainer.getContainerId(), infinispanContainer::close,
                    infinispanContainer.getHost() + ":" + infinispanContainer.getPort(),
                    infinispanContainer.getUser(), infinispanContainer.getPassword());
        };

        return infinispanContainerLocator.locateContainer(devServicesConfig.serviceName, devServicesConfig.shared, launchMode)
                .map(containerAddress -> getRunningDevService(containerAddress.getId(), null,
                        containerAddress.getUrl(), DEFAULT_USERNAME, DEFAULT_PASSWORD)) // TODO can this be always right ?
                .orElseGet(defaultInfinispanServerSupplier);
    }

    @NotNull
    private RunningDevService getRunningDevService(String containerId, Closeable closeable, String serverList,
            String username, String password) {
        Map<String, String> config = new HashMap<>();
        config.put(getConfigPrefix() + "server-list", serverList);
        config.put(getConfigPrefix() + "client-intelligence", "BASIC");
        config.put(getConfigPrefix() + "auth-username", username);
        config.put(getConfigPrefix() + "auth-password", password);
        return new RunningDevService(Feature.INFINISPAN_CLIENT.getName(), containerId, closeable, config);
    }

    private String getConfigPrefix() {
        return QUARKUS + "infinispan-client" + DOT;
    }

    private static class QuarkusInfinispanContainer extends InfinispanContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusInfinispanContainer(OptionalInt fixedExposedPort, String serviceName, boolean useSharedNetwork,
                Optional<List<String>> artifacts) {
            super();
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            withUser(DEFAULT_USERNAME);
            withPassword(InfinispanDevServiceProcessor.DEFAULT_PASSWORD);
            artifacts.ifPresent(a -> withArtifacts(a.toArray(new String[0])));
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
