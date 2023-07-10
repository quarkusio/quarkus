package io.quarkus.opentelemetry.deployment.devservices;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
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

    private static final String OTEL_CONFIG_ENDPOINT = "quarkus.otel.exporter.otlp.traces.endpoint";

    private static final String JAEGER_SCHEME = "http://";

    private static final String JAEGER_ENDPOINT = "/api/traces";

    private static final int JAEGER_CONSOLE_PORT = 16686;

    private static final int OTEL_EXPORTER_PORT = 4317;

    /**
     * Label to add to shared Dev Service for Jaeger running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-otel";

    private static final ContainerLocator jaegerContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, OTEL_EXPORTER_PORT);
    private static volatile RunningDevService devService;
    private static volatile DevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startJaegerContainer(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            OtelBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        DevServicesConfig currentDevServicesConfiguration = config.devservices();

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
        String jaegerConsole = jaegerHost.substring(0, jaegerHost.lastIndexOf(':') + 1)
                + currentDevServicesConfiguration.consolePort();
        log.info("Dev service for Jaeger started. Accessible on " + jaegerConsole);

        return devService.toBuildItem();
    }

    private RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!capturedDevServicesConfiguration.enabled()) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Jaeger as it has been disabled in the config");
            return null;
        }

        if (ConfigUtils.isPropertyPresent(OTEL_CONFIG_ENDPOINT)) {
            log.debug("Not starting Dev Services for Jaeger as '" + OTEL_CONFIG_ENDPOINT + "' has been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Please configure '" + OTEL_CONFIG_ENDPOINT + "' or get a working docker instance");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName
                .parse(capturedDevServicesConfiguration.imageName().get());

        final Supplier<RunningDevService> defaultJaegerSupplier = () -> {
            QuarkusJaegerContainer jaegerContainer = new QuarkusJaegerContainer(dockerImageName,
                    capturedDevServicesConfiguration.consolePort(),
                    capturedDevServicesConfiguration.exporterPort(),
                    capturedDevServicesConfiguration.serviceName(), useSharedNetwork);
            timeout.ifPresent(jaegerContainer::withStartupTimeout);
            jaegerContainer.start();

            String jaegerHost = JAEGER_SCHEME + jaegerContainer.getHost() + ":" + jaegerContainer.getPort();

            return new RunningDevService(Feature.OPENTELEMETRY_JAEGER_EXPORTER.getName(), jaegerContainer.getContainerId(),
                    new ContainerShutdownCloseable(jaegerContainer, "Jaeger"),
                    OTEL_CONFIG_ENDPOINT, jaegerHost);
        };

        return jaegerContainerLocator
                .locateContainer(capturedDevServicesConfiguration.serviceName(), capturedDevServicesConfiguration.shared(),
                        LaunchMode.current())
                .map(containerAddress -> {
                    String jaegerUrl = JAEGER_SCHEME + containerAddress.getUrl();
                    return new RunningDevService(Feature.OPENTELEMETRY_JAEGER_EXPORTER.getName(), containerAddress.getId(),
                            null, OTEL_CONFIG_ENDPOINT, jaegerUrl);
                })
                .orElseGet(defaultJaegerSupplier);
    }

    private static class QuarkusJaegerContainer extends GenericContainer<QuarkusJaegerContainer> {
        private final Integer consolePort;

        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusJaegerContainer(DockerImageName dockerImageName, Integer consolePort, OptionalInt fixedExposedPort,
                String serviceName, boolean useSharedNetwork) {
            super(dockerImageName);
            this.consolePort = consolePort;
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
        protected void configure() {
            super.configure();

            addEnv("COLLECTOR_OTLP_ENABLED", "true");
            withReuse(true);

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "jaeger");
                return;
            }

            addFixedExposedPort(consolePort, JAEGER_CONSOLE_PORT);

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), OTEL_EXPORTER_PORT);
            } else {
                addExposedPort(OTEL_EXPORTER_PORT);
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return OTEL_EXPORTER_PORT;
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
