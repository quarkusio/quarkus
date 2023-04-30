package io.quarkus.jaeger.deployment.devservices;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class })
public class DevServicesJaegerProcessor {
    private static final Logger log = Logger.getLogger(DevServicesJaegerProcessor.class);

    private static final String JAEGER_IMAGE = "quay.io/jaegertracing/all-in-one:1.44.0";

    private static final String CONFIG_PREFIX = "quarkus.jaeger.";

    private static final String ENDPOINT_CONFIG_KEY = "endpoint";

    private static final String JAEGER_SCHEME = "http://";

    private static final String JAEGER_ENDPOINT = "/api/traces";

    private static final int JAEGER_EXPOSED_PORT = 14268;

    private static final int JAEGER_CONSOLE_PORT = 16686;

    /**
     * Label to add to shared Dev Service for Jaeger running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-jaeger";

    private static final ContainerLocator jaegerContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, JAEGER_EXPOSED_PORT);
    private static volatile RunningDevService devService;
    private static volatile DevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startJaegerContainer(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            JaegerBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        DevServicesConfig currentDevServicesConfiguration = config.devservices;

        if (devService != null) {
            boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return devService.toBuildItem();
            }
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop Jaeger container", e);
            }

            devService = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = currentDevServicesConfiguration;

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Jaeger Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService newDevService = startContainer(dockerStatusBuildItem,
                    !devServicesSharedNetworkBuildItem.isEmpty(), devServicesConfig.timeout);
            if (newDevService == null) {
                compressor.closeAndDumpCaptured();
                return null;
            } else {
                compressor.close();
            }

            devService = newDevService;

        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    try {
                        devService.close();
                    } catch (Throwable t) {
                        log.error("Failed to stop Jaeger container", t);
                    }
                }
                first = true;
                devService = null;
                capturedDevServicesConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }

        String jaegerHost = devService.getConfig().values().stream().findFirst().get();
        String jaegerConsole = jaegerHost.substring(0, jaegerHost.lastIndexOf(':') + 1) + JAEGER_CONSOLE_PORT;
        log.info("Dev service for Jaeger started. Accessible on " + jaegerConsole);

        return devService.toBuildItem();
    }

    private RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!capturedDevServicesConfiguration.enabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Jaeger as it has been disabled in the config");
            return null;
        }

        if (ConfigUtils.isPropertyPresent(ENDPOINT_CONFIG_KEY)) {
            log.debug("Not starting Dev Services for Jaeger as 'quarkus.jaeger.endpoint' has been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Please configure 'quarkus.jaeger.endpoint' or get a working docker instance");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName.parse(capturedDevServicesConfiguration.imageName.orElse(JAEGER_IMAGE))
                .asCompatibleSubstituteFor(JAEGER_IMAGE);

        final Supplier<RunningDevService> defaultJaegerSupplier = () -> {
            QuarkusJaegerContainer jaegerContainer = new QuarkusJaegerContainer(dockerImageName,
                    capturedDevServicesConfiguration.port,
                    capturedDevServicesConfiguration.serviceName, useSharedNetwork);
            timeout.ifPresent(jaegerContainer::withStartupTimeout);
            jaegerContainer.start();

            String jaegerHost = JAEGER_SCHEME + jaegerContainer.getHost() + ":" + jaegerContainer.getPort() + JAEGER_ENDPOINT;

            return new RunningDevService(Feature.JAEGER.getName(), jaegerContainer.getContainerId(),
                    new ContainerShutdownCloseable(jaegerContainer, "Jaeger"),
                    CONFIG_PREFIX + ENDPOINT_CONFIG_KEY, jaegerHost);
        };

        return jaegerContainerLocator
                .locateContainer(capturedDevServicesConfiguration.serviceName, capturedDevServicesConfiguration.shared,
                        LaunchMode.current())
                .map(containerAddress -> {
                    String jaegerUrl = JAEGER_SCHEME + containerAddress.getUrl() + JAEGER_ENDPOINT;
                    return new RunningDevService(Feature.JAEGER.getName(), containerAddress.getId(),
                            null, CONFIG_PREFIX + ENDPOINT_CONFIG_KEY, jaegerUrl);
                })
                .orElseGet(defaultJaegerSupplier);
    }

    private static class QuarkusJaegerContainer extends GenericContainer<QuarkusJaegerContainer> {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusJaegerContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            this.waitStrategy = (new LogMessageWaitStrategy()).withRegEx(
                    ".*\"Health Check state change\",\"status\":\"ready\".*")
                    .withStartupTimeout(Duration.of(15L, ChronoUnit.SECONDS));

            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
        }

        @Override
        public void close() {

        }

        @Override
        protected void configure() {
            super.configure();

            addFixedExposedPort(5775, 5775, InternetProtocol.UDP);
            addFixedExposedPort(6831, 6831, InternetProtocol.UDP);
            addFixedExposedPort(6832, 6832, InternetProtocol.UDP);
            addFixedExposedPort(5778, 5778);
            addFixedExposedPort(16686, 16686);
            withReuse(true);

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "jaeger");
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), JAEGER_EXPOSED_PORT);
            } else {
                addFixedExposedPort(JAEGER_EXPOSED_PORT, JAEGER_EXPOSED_PORT);
                addExposedPort(JAEGER_EXPOSED_PORT);
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return JAEGER_EXPOSED_PORT;
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
    }
}
