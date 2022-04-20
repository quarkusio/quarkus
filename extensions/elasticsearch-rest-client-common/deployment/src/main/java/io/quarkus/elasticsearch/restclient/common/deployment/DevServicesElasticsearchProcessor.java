package io.quarkus.elasticsearch.restclient.common.deployment;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts an Elasticsearch server as dev service if needed.
 */
public class DevServicesElasticsearchProcessor {
    private static final Logger log = Logger.getLogger(DevServicesElasticsearchProcessor.class);

    /**
     * Label to add to shared Dev Service for Elasticsearch running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-elasticsearch";
    static final int ELASTICSEARCH_PORT = 9200;

    private static final ContainerLocator elasticsearchContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            ELASTICSEARCH_PORT);

    static volatile DevServicesResultBuildItem.RunningDevService devService;
    static volatile ElasticsearchDevServicesBuildTimeConfig cfg;
    static volatile boolean first = true;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem startElasticsearchDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            ElasticsearchDevServicesBuildTimeConfig configuration,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig,
            List<DevservicesElasticsearchBuildItem> devservicesElasticsearchBuildItems) throws BuildException {

        if (devservicesElasticsearchBuildItems.isEmpty()) {
            // safety belt in case a module depends on this one without producing the build item
            return null;
        }

        DevservicesElasticsearchBuildItemsConfiguration buildItemsConfig = new DevservicesElasticsearchBuildItemsConfiguration(
                devservicesElasticsearchBuildItems);

        if (devService != null) {
            boolean shouldShutdownTheServer = !configuration.equals(cfg);
            if (!shouldShutdownTheServer) {
                return devService.toBuildItem();
            }
            shutdownElasticsearch();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Elasticsearch Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            devService = startElasticsearch(dockerStatusBuildItem, configuration, buildItemsConfig, launchMode,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout);
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownElasticsearch();
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        if (devService.isOwner()) {
            log.infof(
                    "Dev Services for Elasticsearch started. Other Quarkus applications in dev mode will find the "
                            + "server automatically. For Quarkus applications in production mode, you can connect to"
                            + " this by configuring your application to use %s",
                    getElasticsearchHosts(buildItemsConfig));
        }
        return devService.toBuildItem();
    }

    public static String getElasticsearchHosts(DevservicesElasticsearchBuildItemsConfiguration buildItemsConfiguration) {
        String hostsConfigProperty = buildItemsConfiguration.hostsConfigProperties.stream().findAny().get();
        return devService.getConfig().get(hostsConfigProperty);
    }

    private void shutdownElasticsearch() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Elasticsearch server", e);
            } finally {
                devService = null;
            }
        }
    }

    private DevServicesResultBuildItem.RunningDevService startElasticsearch(
            DockerStatusBuildItem dockerStatusBuildItem,
            ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout) throws BuildException {
        if (!config.enabled.orElse(true)) {
            // explicitly disabled
            log.debug("Not starting dev services for Elasticsearch, as it has been disabled in the config.");
            return null;
        }

        for (String hostsConfigProperty : buildItemConfig.hostsConfigProperties) {
            // Check if elasticsearch hosts property is set
            if (ConfigUtils.isPropertyPresent(hostsConfigProperty)) {
                log.debugf("Not starting dev services for Elasticsearch, the %s property is configured.", hostsConfigProperty);
                return null;
            }
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warnf("Docker isn't working, please configure the Elasticsearch hosts property (%s).",
                    displayProperties(buildItemConfig.hostsConfigProperties));
            return null;
        }

        // We only support ELASTIC container for now
        if (buildItemConfig.distribution == DevservicesElasticsearchBuildItem.Distribution.OPENSEARCH) {
            throw new BuildException("Dev services for Elasticsearch didn't support Opensearch", Collections.emptyList());
        }

        // Hibernate search Elasticsearch have a version configuration property, we need to check that it is coherent
        // with the image we are about to launch
        if (buildItemConfig.version != null) {
            String containerTag = config.imageName.substring(config.imageName.indexOf(':') + 1);
            if (!containerTag.startsWith(buildItemConfig.version)) {
                throw new BuildException(
                        "Dev services for Elasticsearch detected a version mismatch, container image is " + config.imageName
                                + " but the configured version is " + buildItemConfig.version +
                                ". Either configure a different image or disable dev services for Elasticsearch.",
                        Collections.emptyList());
            }
        }

        final Optional<ContainerAddress> maybeContainerAddress = elasticsearchContainerLocator.locateContainer(
                config.serviceName,
                config.shared,
                launchMode.getLaunchMode());

        // Starting the server
        final Supplier<DevServicesResultBuildItem.RunningDevService> defaultElasticsearchSupplier = () -> {
            ElasticsearchContainer container = new ElasticsearchContainer(
                    DockerImageName.parse(config.imageName));
            ConfigureUtil.configureSharedNetwork(container, "elasticsearch");
            if (config.serviceName != null) {
                container.withLabel(DEV_SERVICE_LABEL, config.serviceName);
            }
            if (config.port.isPresent()) {
                container.setPortBindings(List.of(config.port.get() + ":" + config.port.get()));
            }
            timeout.ifPresent(container::withStartupTimeout);
            container.addEnv("ES_JAVA_OPTS", config.javaOpts);
            // Disable security as else we would need to configure it correctly to avoid tons of WARNING in the log
            container.addEnv("xpack.security.enabled", "false");

            container.start();
            return new DevServicesResultBuildItem.RunningDevService(Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                    container.getContainerId(),
                    container::close,
                    buildPropertiesMap(buildItemConfig, container.getHttpHostAddress()));
        };

        return maybeContainerAddress
                .map(containerAddress -> new DevServicesResultBuildItem.RunningDevService(
                        Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                        containerAddress.getId(),
                        null,
                        buildPropertiesMap(buildItemConfig, containerAddress.getUrl())))
                .orElseGet(defaultElasticsearchSupplier);
    }

    private Map<String, String> buildPropertiesMap(DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            String httpHosts) {
        Map<String, String> propertiesToSet = new HashMap<>();
        for (String property : buildItemConfig.hostsConfigProperties) {
            propertiesToSet.put(property, httpHosts);
        }
        return propertiesToSet;
    }

    private String displayProperties(Set<String> hostsConfigProperties) {
        return String.join(" and ", hostsConfigProperties);
    }

    private static class DevservicesElasticsearchBuildItemsConfiguration {
        private Set<String> hostsConfigProperties;
        private String version;
        private DevservicesElasticsearchBuildItem.Distribution distribution;

        private DevservicesElasticsearchBuildItemsConfiguration(List<DevservicesElasticsearchBuildItem> buildItems)
                throws BuildException {
            hostsConfigProperties = new HashSet<>(buildItems.size());

            // check that all build items agree on the version and distribution to start
            for (DevservicesElasticsearchBuildItem buildItem : buildItems) {
                if (version == null) {
                    version = buildItem.getVersion();
                } else if (!version.equals(buildItem.getVersion())) {
                    // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the version
                    throw new BuildException("Multiple extensions request Elasticsearch Dev Services on different version.",
                            Collections.emptyList());
                }

                if (distribution == null) {
                    distribution = buildItem.getDistribution();
                } else if (!distribution.equals(buildItem.getDistribution())) {
                    // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the distribution
                    throw new BuildException(
                            "Multiple extensions request Elasticsearch Dev Services on different distribution.",
                            Collections.emptyList());
                }

                hostsConfigProperties.add(buildItem.getHostsConfigProperty());
            }
        }
    }
}
