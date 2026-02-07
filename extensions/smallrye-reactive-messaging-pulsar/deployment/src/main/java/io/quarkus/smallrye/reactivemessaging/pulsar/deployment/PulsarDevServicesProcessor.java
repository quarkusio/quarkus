package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a Pulsar broker as dev service if needed.
 * It uses https://hub.docker.com/r/apachepulsar/pulsar as image.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class PulsarDevServicesProcessor {

    private static final Logger log = Logger.getLogger(PulsarDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for pulsar running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-pulsar";

    private static final ContainerLocator pulsarContainerLocator = locateContainerWithLabels(PulsarContainer.BROKER_PORT,
            DEV_SERVICE_LABEL);

    private static final String PULSAR_CLIENT_SERVICE_URL = "pulsar.client.serviceUrl";
    private static final String PULSAR_ADMIN_SERVICE_URL = "pulsar.admin.serviceUrl";
    static final String DEV_SERVICE_PULSAR = "pulsar";

    @BuildStep
    public DevServicesResultBuildItem startPulsarDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            PulsarBuildTimeConfig pulsarBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig) {

        PulsarDevServicesBuildTimeConfig config = pulsarBuildTimeConfig.devservices();
        if (devServiceDisabled(dockerStatusBuildItem, config.enabled().orElse(true))) {
            return null;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                sharedNetwork);

        return pulsarContainerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .map(containerAddress -> {
                    int httpPort = pulsarContainerLocator
                            .locatePublicPort(config.serviceName(), config.shared(),
                                    launchMode.getLaunchMode(), PulsarContainer.BROKER_HTTP_PORT)
                            .orElse(8080);
                    String pulsarUrl = String.format("pulsar://%s:%d", containerAddress.getHost(), containerAddress.getPort());
                    String httpUrl = String.format("http://%s:%d", containerAddress.getHost(), httpPort);
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.MESSAGING_PULSAR)
                            .containerId(containerAddress.getId())
                            .config(Map.of(
                                    PULSAR_CLIENT_SERVICE_URL, pulsarUrl,
                                    PULSAR_ADMIN_SERVICE_URL, httpUrl))
                            .build();
                })
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of(config.imageName().orElse(getDefaultImageNameFor("pulsar")), "pulsar"),
                        PulsarContainer.BROKER_PORT, launchMode.getLaunchMode(), useSharedNetwork)
                        .map(containerAddress -> {
                            RunningContainer container = containerAddress.getRunningContainer();
                            if (container == null) {
                                return null;
                            }
                            int httpPort = container.getPortMapping(PulsarContainer.BROKER_HTTP_PORT).orElse(8080);
                            String pulsarUrl = String.format("pulsar://%s:%d", containerAddress.getHost(),
                                    containerAddress.getPort());
                            String httpUrl = String.format("http://%s:%d", containerAddress.getHost(), httpPort);
                            return DevServicesResultBuildItem.discovered()
                                    .feature(Feature.MESSAGING_PULSAR)
                                    .containerId(containerAddress.getId())
                                    .config(Map.of(
                                            PULSAR_CLIENT_SERVICE_URL, pulsarUrl,
                                            PULSAR_ADMIN_SERVICE_URL, httpUrl))
                                    .build();
                        }))
                .orElseGet(() -> {
                    PulsarContainer container = new PulsarContainer(
                            DockerImageName.parse(config.imageName().orElse(getDefaultImageNameFor("pulsar")))
                                    .asCompatibleSubstituteFor("apachepulsar/pulsar"),
                            compose.getDefaultNetworkId(),
                            useSharedNetwork);

                    // Apply broker configuration
                    config.brokerConfig().forEach((key, value) -> container.addEnv("PULSAR_PREFIX_" + key, value));

                    // Add labels in dev mode
                    if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
                        container.withLabel(DEV_SERVICE_LABEL, config.serviceName());
                        container.withLabel(QUARKUS_DEV_SERVICE, config.serviceName());
                    }

                    // Set fixed port if configured
                    if (config.port().orElse(0) != 0) {
                        container.withPort(config.port().get());
                    }

                    return DevServicesResultBuildItem.owned()
                            .feature(Feature.MESSAGING_PULSAR)
                            .serviceConfig(config)
                            .startable(() -> container)
                            .postStartHook(this::logStarted)
                            .configProvider(Map.of(
                                    PULSAR_CLIENT_SERVICE_URL, PulsarContainer::getPulsarBrokerUrl,
                                    PULSAR_ADMIN_SERVICE_URL, PulsarContainer::getHttpServiceUrl))
                            .build();
                });
    }

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem, boolean devServicesEnabled) {
        if (!devServicesEnabled) {
            log.debug("Not starting Dev Services for Pulsar, as it has been disabled in the config.");
            return true;
        }

        if (ConfigUtils.isPropertyNonEmpty(PULSAR_CLIENT_SERVICE_URL)) {
            log.debug("Not starting Dev Services for Pulsar, the pulsar.serviceUrl is configured.");
            return true;
        }

        if (!hasPulsarChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for Pulsar, all the channels are configured.");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please configure the Pulsar broker location.");
            return true;
        }

        return false;
    }

    private void logStarted(PulsarContainer container) {
        log.info("Dev Services for Pulsar started.");
        log.infof("Other Quarkus applications in dev mode will find the "
                + "broker automatically. For Quarkus applications in production mode, you can connect to"
                + " this by starting your application with -Dpulsar.client.serviceUrl=%s",
                container.getPulsarBrokerUrl());
    }

    private boolean hasPulsarChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isPulsar = connectorValue.equalsIgnoreCase("smallrye-pulsar");
                boolean hasServiceUrl = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".serviceUrl"));
                isConfigured = isPulsar && hasServiceUrl;
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

}
