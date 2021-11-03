package io.quarkus.infinispan.client.deployment.devservices;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.server.test.core.InfinispanContainer;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.Base58;

import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
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
    private static volatile List<Closeable> closeables;
    private static volatile InfinispanClientDevServiceBuildTimeConfig.DevServiceConfiguration capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private static volatile Boolean dockerRunning = null;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class })
    public void startInfinispanContainers(LaunchModeBuildItem launchMode,
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            BuildProducer<DevServicesConfigResultBuildItem> devConfigProducer, InfinispanClientDevServiceBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, GlobalDevServicesConfig devServicesConfig) {

        // figure out if we need to shut down and restart existing Infinispan containers
        // if not and the Infinispan containers have already started we just return
        if (closeables != null) {
            boolean restartRequired = !config.devService.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return;
            }
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    log.error("Failed to stop infinispan container", e);
                }
            }
            closeables = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = config.devService;
        List<Closeable> currentCloseables = new ArrayList<>();

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Infinispan Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            StartResult startResult = startContainer(config.devService.devservices,
                    launchMode.getLaunchMode(),
                    devServicesSharedNetworkBuildItem.isPresent(), devServicesConfig.timeout);
            if (startResult == null) {
                compressor.close();
                return;
            }
            currentCloseables.add(startResult.closeable);

            devConfigProducer
                    .produce(new DevServicesConfigResultBuildItem(getConfigPrefix() + "server-list", startResult.serverList));
            devConfigProducer.produce(new DevServicesConfigResultBuildItem(getConfigPrefix() + "client-intelligence", "BASIC"));
            devConfigProducer
                    .produce(new DevServicesConfigResultBuildItem(getConfigPrefix() + "auth-username", startResult.user));
            devConfigProducer
                    .produce(new DevServicesConfigResultBuildItem(getConfigPrefix() + "auth-password", startResult.password));
            log.infof("The infinispan server is ready to accept connections on %s", startResult.serverList);
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        closeables = currentCloseables;

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                dockerRunning = null;
                if (closeables != null) {
                    for (Closeable closeable : closeables) {
                        try {
                            closeable.close();
                        } catch (Throwable t) {
                            log.error("Failed to stop infinispan", t);
                        }
                    }
                }
                first = true;
                closeables = null;
                capturedDevServicesConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
    }

    private StartResult startContainer(
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

        if (dockerRunning == null) {
            dockerRunning = new IsDockerWorking.IsDockerRunningSilent().getAsBoolean();
        }

        if (!dockerRunning) {
            log.warn("Please configure 'quarkus.infinispan-client.server-list' or get a working docker instance");
            return null;
        }

        Supplier<StartResult> defaultInfinispanServerSupplier = () -> {
            QuarkusInfinispanContainer infinispanContainer = new QuarkusInfinispanContainer(devServicesConfig.port,
                    launchMode == DEVELOPMENT ? devServicesConfig.serviceName : null, useSharedNetwork);
            timeout.ifPresent(infinispanContainer::withStartupTimeout);
            infinispanContainer.start();
            String serverList = infinispanContainer.getHost() + ":" + infinispanContainer.getPort();
            String user = infinispanContainer.getUser();
            String secret = infinispanContainer.getPassword();
            return new StartResult(serverList, user, secret, () -> infinispanContainer.close());
        };

        return infinispanContainerLocator.locateContainer(devServicesConfig.serviceName, devServicesConfig.shared, launchMode)
                .map(containerAddress -> new StartResult(containerAddress.getUrl(),
                        InfinispanContainer.DEFAULT_USERNAME, DEFAULT_PASSWORD, null))
                .orElseGet(defaultInfinispanServerSupplier);

    }

    private String getConfigPrefix() {
        return QUARKUS + "infinispan-client" + DOT;
    }

    private static class StartResult {
        private final String serverList;
        private final String user;
        private final String password;
        private final Closeable closeable;

        public StartResult(String sl, String user, String password, Closeable closeable) {
            this.serverList = sl;
            this.user = user;
            this.password = password;
            this.closeable = closeable;
        }
    }

    private static class QuarkusInfinispanContainer extends InfinispanContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusInfinispanContainer(OptionalInt fixedExposedPort, String serviceName, boolean useSharedNetwork) {
            super();
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            withUser(DEFAULT_USERNAME);
            withPassword(InfinispanDevServiceProcessor.DEFAULT_PASSWORD);
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                // When a shared network is requested for the launched containers, we need to configure
                // the container to use it. We also need to create a hostname that will be applied to the returned
                // Infinispan URL
                setNetwork(Network.SHARED);
                hostName = "infinispan-" + Base58.randomString(5);
                setNetworkAliases(Collections.singletonList(hostName));
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
            return InfinispanContainer.DEFAULT_USERNAME;
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
