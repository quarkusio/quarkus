package io.quarkus.infinispan.client.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static org.infinispan.testcontainers.InfinispanContainer.DEFAULT_USERNAME;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.testcontainers.InfinispanContainer;
import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.infinispan.client.runtime.InfinispanClientBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.infinispan.client.runtime.InfinispanClientsBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanDevServicesConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
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
    private static final String DOT = ".";
    private static final String UNDERSCORE = "_";

    @BuildStep
    public void startInfinispanContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            InfinispanClientsBuildTimeConfig config,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServices) {

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        DevServicesResultBuildItem defaultResult = prepareInfinispanDevService(
                InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME,
                launchMode.getLaunchMode(),
                dockerStatusBuildItem,
                composeProjectBuildItem,
                config.defaultInfinispanClient(),
                devServicesConfig,
                useSharedNetwork);
        if (defaultResult != null) {
            devServices.produce(defaultResult);
        }

        for (Map.Entry<String, InfinispanClientBuildTimeConfig> entry : config.namedInfinispanClients().entrySet()) {
            DevServicesResultBuildItem namedResult = prepareInfinispanDevService(
                    entry.getKey(),
                    launchMode.getLaunchMode(),
                    dockerStatusBuildItem,
                    composeProjectBuildItem,
                    entry.getValue(),
                    devServicesConfig,
                    useSharedNetwork);
            if (namedResult != null) {
                devServices.produce(namedResult);
            }
        }

    }

    private DevServicesResultBuildItem prepareInfinispanDevService(String clientName,
            LaunchMode launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            InfinispanClientBuildTimeConfig config,
            DevServicesConfig devServicesConfig,
            boolean useSharedNetwork) {
        try {
            InfinispanDevServicesConfig namedDevServiceConfig = config.devservices().devservices();

            if (!namedDevServiceConfig.enabled()) {
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
            log.infof("Applying Dev Services config %s", namedDevServiceConfig);

            return infinispanContainerLocator
                    .locateContainer(namedDevServiceConfig.serviceName(), namedDevServiceConfig.shared(), launchMode)
                    .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                            List.of(namedDevServiceConfig.imageName().orElseGet(() -> getDefaultImageNameFor("infinispan")),
                                    "infinispan", "datagrid"),
                            DEFAULT_INFINISPAN_PORT, launchMode, useSharedNetwork))
                    .map(containerAddress -> {
                        RunningContainer container = containerAddress.getRunningContainer();
                        String username = container != null ? container.tryGetEnv("USER").orElse(DEFAULT_USERNAME)
                                : DEFAULT_USERNAME;
                        String password = container != null ? container.tryGetEnv("PASS").orElse(DEFAULT_PASSWORD)
                                : DEFAULT_PASSWORD;

                        log.infof("The infinispan server is ready to accept connections on %s", containerAddress.getUrl());

                        return DevServicesResultBuildItem.discovered()
                                .feature(Feature.INFINISPAN_CLIENT)
                                .containerId(containerAddress.getId())
                                .config(Map.of(
                                        configPrefix + "hosts", containerAddress.getUrl(),
                                        configPrefix + "username", username,
                                        configPrefix + "password", password))
                                .build();
                    })
                    .orElseGet(() -> DevServicesResultBuildItem.owned()
                            .feature(Feature.INFINISPAN_CLIENT)
                            .serviceName(namedDevServiceConfig.serviceName())
                            .serviceConfig(namedDevServiceConfig)
                            .startable(() -> createContainer(clientName, namedDevServiceConfig, launchMode,
                                    composeProjectBuildItem.getDefaultNetworkId(),
                                    useSharedNetwork, devServicesConfig.timeout()))
                            .postStartHook(s -> logStarted(s.getConnectionInfo()))
                            .configProvider(Map.of(
                                    configPrefix + "hosts", Startable::getConnectionInfo,
                                    configPrefix + "username", s -> DEFAULT_USERNAME,
                                    configPrefix + "password", s -> DEFAULT_PASSWORD))
                            .build());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void logStarted(String hosts) {
        log.infof("The infinispan server is ready to accept connections on %s", hosts);
    }

    private QuarkusInfinispanContainer createContainer(String clientName, InfinispanDevServicesConfig devServicesConfig,
            LaunchMode launchMode, String defaultNetworkId, boolean useSharedNetwork, Optional<Duration> timeout) {
        QuarkusInfinispanContainer infinispanContainer = new QuarkusInfinispanContainer(clientName, devServicesConfig,
                launchMode,
                defaultNetworkId,
                useSharedNetwork);
        timeout.ifPresent(infinispanContainer::withStartupTimeout);
        infinispanContainer.withEnv(devServicesConfig.containerEnv());
        return infinispanContainer;
    }

    private String getConfigPrefix(String name) {
        if (name.equals(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME)) {
            return InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_MAPPING_PREFIX + DOT;
        }

        return InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_MAPPING_PREFIX + DOT + name + DOT;
    }

    private static class QuarkusInfinispanContainer extends InfinispanContainer implements Startable {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusInfinispanContainer(String clientName, InfinispanDevServicesConfig config,
                LaunchMode launchMode, String defaultNetworkId, boolean useSharedNetwork) {
            super(config.imageName().orElseGet(() -> getDefaultImageNameFor("infinispan")));
            this.fixedExposedPort = config.port();
            this.useSharedNetwork = useSharedNetwork;

            String serviceName = config.serviceName();
            if (InfinispanClientUtil.DEFAULT_INFINISPAN_DEV_SERVICE_NAME.equals(serviceName)
                    && !InfinispanClientUtil.isDefault(clientName)) {
                // Adds the client name suffix to create a different service name in named connections
                serviceName = serviceName + UNDERSCORE + clientName;
            }
            configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);

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
                                "You need to use the `quarkus.infinispan-client.devservices.config-files` property and provide a JSON, XML or YAML file as follows. Check https://quarkus.io/guides/infinispan-dev-services for more information");
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
            setWaitStrategy(Wait.forLogMessage(".*Infinispan Server.*started.*", 1));
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

        @Override
        public String getHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }

        @Override
        public String getConnectionInfo() {
            return getHost() + ":" + getPort();
        }

        @Override
        public void close() {
            super.close();
        }
    }
}
