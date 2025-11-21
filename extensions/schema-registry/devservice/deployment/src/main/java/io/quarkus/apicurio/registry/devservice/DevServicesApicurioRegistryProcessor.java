package io.quarkus.apicurio.registry.devservice;

import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.apicurio.registry.devservice.ApicurioRegistryBuildTimeConfig.ApicurioRegistryDevServicesBuildTimeConfig;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts Apicurio Registry as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesApicurioRegistryProcessor {

    private static final Logger log = Logger.getLogger(DevServicesApicurioRegistryProcessor.class);

    private static final int APICURIO_REGISTRY_PORT = 8080; // inside the container
    private static final String APICURIO_REGISTRY_URL_CONFIG = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url";
    private static final String CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG = "mp.messaging.connector.smallrye-kafka.schema.registry.url";

    /**
     * Label to add to shared Dev Service for Apicurio Registry running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-apicurio-registry";

    private static final ContainerLocator apicurioRegistryContainerLocator = locateContainerWithLabels(APICURIO_REGISTRY_PORT,
            DEV_SERVICE_LABEL);

    @BuildStep
    public DevServicesResultBuildItem startApicurioRegistryDevService(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ApicurioRegistryBuildTimeConfig apicurioRegistryConfiguration,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesConfig devServicesConfig) {
        ApicurioRegistryDevServicesBuildTimeConfig cfg = apicurioRegistryConfiguration.devservices();
        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);
        return prepareApicurioRegistry(dockerStatusBuildItem, composeProjectBuildItem, cfg, launchMode,
                useSharedNetwork, devServicesConfig.timeout());
    }

    private Map<String, String> getRegistryUrlConfigs(String baseUrl) {
        return Map.of(
                APICURIO_REGISTRY_URL_CONFIG, baseUrl + "/apis/registry/v3",
                CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, baseUrl + "/apis/ccompat/v7");
    }

    private DevServicesResultBuildItem prepareApicurioRegistry(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ApicurioRegistryDevServicesBuildTimeConfig config,
            LaunchModeBuildItem launchMode,
            boolean useSharedNetwork,
            Optional<Duration> timeout) {
        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting dev services for Apicurio Registry, as it has been disabled in the config.");
            return null;
        }

        if (ConfigUtils.isPropertyNonEmpty(APICURIO_REGISTRY_URL_CONFIG)) {
            log.debug("Not starting dev services for Apicurio Registry, " + APICURIO_REGISTRY_URL_CONFIG + " is configured.");
            return null;
        }

        if (ConfigUtils.isPropertyNonEmpty(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG)) {
            log.debug("Not starting dev services for Apicurio Registry, " + CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG
                    + " is configured.");
            return null;
        }

        if (!hasKafkaChannelWithoutRegistry()) {
            log.debug("Not starting dev services for Apicurio Registry, all the channels have a registry URL configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please run Apicurio Registry yourself.");
            return null;
        }

        return apicurioRegistryContainerLocator
                .locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(config.imageName().orElseGet(() -> getDefaultImageNameFor("apicurio-registry")), "apicurio"),
                        APICURIO_REGISTRY_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(address -> DevServicesResultBuildItem.discovered()
                        .feature(Feature.APICURIO_REGISTRY_AVRO)
                        .containerId(address.getId())
                        // address does not have the URL Scheme - just the host:port, so prepend http://
                        .config(getRegistryUrlConfigs("http://" + address.getUrl()))
                        .build())
                .orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(Feature.APICURIO_REGISTRY_AVRO)
                        .startable(() -> new ApicurioRegistryContainer(
                                DockerImageName
                                        .parse(config.imageName().orElseGet(() -> getDefaultImageNameFor("apicurio-registry")))
                                        .asCompatibleSubstituteFor("apicurio/apicurio-registry-mem"),
                                config.port().orElse(0),
                                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName() : null,
                                composeProjectBuildItem.getDefaultNetworkId(),
                                useSharedNetwork,
                                config.containerEnv(),
                                timeout))
                        .configProvider(Map.of(
                                APICURIO_REGISTRY_URL_CONFIG, ApicurioRegistryContainer::getApicurioRegistryUrl,
                                CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, ApicurioRegistryContainer::getConfluentRegistryUrl))
                        .postStartHook(
                                s -> log.infof("Dev Services for Apicurio Registry started. The registry is available at %s",
                                        s.getApicurioRegistryUrl()))
                        .build());
    }

    private boolean hasKafkaChannelWithoutRegistry() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isKafka = isConnector
                    && "smallrye-kafka".equals(config.getOptionalValue(name, String.class).orElse("ignored"));
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isKafka) {
                isConfigured = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".apicurio.registry.url"))
                        || ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".schema.registry.url"));
            }
            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private static final class ApicurioRegistryContainer extends GenericContainer<ApicurioRegistryContainer>
            implements Startable {
        private final int fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        private final boolean legacyImage;

        private ApicurioRegistryContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
                String defaultNetworkId, boolean useSharedNetwork,
                Map<String, String> containerEnv,
                Optional<Duration> timeout) {
            super(dockerImageName);
            this.legacyImage = dockerImageName.getVersionPart().startsWith("2.");
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
            }
            withEnv("QUARKUS_PROFILE", "prod");
            if (!dockerImageName.getRepository().contains("apicurio/apicurio-registry")) {
                throw new IllegalArgumentException("Only apicurio/apicurio-registry images are supported");
            }
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "apicurio-registry");
            withEnv(containerEnv);
            timeout.ifPresent(this::withStartupTimeout);
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort > 0) {
                addFixedExposedPort(fixedExposedPort, APICURIO_REGISTRY_PORT);
            } else {
                addExposedPorts(APICURIO_REGISTRY_PORT);
            }
        }

        @Override
        public void close() {
            super.close();
        }

        public String getUrl() {
            return String.format("http://%s:%s", getHostToUse(), getPortToUse());
        }

        private String getHostToUse() {
            return useSharedNetwork ? hostName : getHost();
        }

        private int getPortToUse() {
            return useSharedNetwork ? APICURIO_REGISTRY_PORT : getMappedPort(APICURIO_REGISTRY_PORT);
        }

        @Override
        public String getConnectionInfo() {
            return getApicurioRegistryUrl();
        }

        public String getApicurioRegistryUrl() {
            return getUrl() + (legacyImage ? "/apis/registry/v2" : "/apis/registry/v3");
        }

        public String getConfluentRegistryUrl() {
            return getUrl() + (legacyImage ? "/apis/ccompat/v6" : "/apis/ccompat/v7");
        }
    }
}
