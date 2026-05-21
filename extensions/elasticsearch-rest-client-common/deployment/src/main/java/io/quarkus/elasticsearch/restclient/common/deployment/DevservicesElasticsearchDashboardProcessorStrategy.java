package io.quarkus.elasticsearch.restclient.common.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.resolveDistribution;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
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
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.devservices.common.Labels;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;
import io.quarkus.runtime.configuration.ConfigUtils;

enum DistributionStrategy {
    DASHBOARDS(Distribution.OPENSEARCH),
    KIBANA(Distribution.ELASTIC);

    private final Distribution distribution;

    private DistributionStrategy(Distribution distribution) {
        this.distribution = distribution;
    }

    public Distribution supportedDistribution() {
        return distribution;
    }
}

public class DevservicesElasticsearchDashboardProcessorStrategy {
    private static final Logger log = Logger.getLogger(DevservicesElasticsearchDashboardProcessorStrategy.class);
    private static final int DASHBOARD_PORT = 5601;
    private static final String DEV_SERVICE_LABEL = "io.quarkus.devservice.elasticsearch.dashboards";

    private static final String DEV_SERVICE_KIBANA = "elasticsearch-kibana";
    private static final String DEV_SERVICE_DASHBOARDS = "opensearch-dashboards";

    private static final ContainerLocator dashboardContainerLocator = locateContainerWithLabels(DASHBOARD_PORT,
            DEV_SERVICE_LABEL);
    volatile RunningDevService devDashboardService;
    volatile ElasticsearchCommonBuildTimeConfig cfg;
    volatile boolean first = true;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final DistributionStrategy strategy;

    private DevservicesElasticsearchDashboardProcessorStrategy(DistributionStrategy strategy) {
        this.strategy = strategy;
    }

    static DevservicesElasticsearchDashboardProcessorStrategy kibana() {
        return new DevservicesElasticsearchDashboardProcessorStrategy(DistributionStrategy.KIBANA);
    }

    static DevservicesElasticsearchDashboardProcessorStrategy dashboards() {
        return new DevservicesElasticsearchDashboardProcessorStrategy(DistributionStrategy.DASHBOARDS);
    }

    public DevServicesResultBuildItem startElasticsearchDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            ElasticsearchCommonBuildTimeConfig configuration,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            List<DevservicesElasticsearchConnectionBuildItem> elasticsearchConnectionBuildItems,
            List<DevservicesElasticsearchBuildItem> devservicesElasticsearchBuildItems,
            DevServicesConfig devServicesConfig) throws BuildException {

        if (devservicesElasticsearchBuildItems.isEmpty() && elasticsearchConnectionBuildItems.isEmpty()) {
            log.info("Skip starting dashboards since no Elasticsearch hosts have been configured");
            // safety belt in case a module depends on this one without producing the build item
            return null;
        }
        DevservicesElasticsearchBuildItemsConfiguration buildItemsConfig = new DevservicesElasticsearchBuildItemsConfiguration(
                devservicesElasticsearchBuildItems);

        if (devDashboardService != null) {
            boolean shouldShutdownTheServer = !configuration.equals(cfg);
            if (!shouldShutdownTheServer) {
                return devDashboardService.toBuildItem();
            }
            shutdownElasticsearchDashboards();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Dev Services for Elasticsearch Dashboards starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);

            devDashboardService = startDashboardDevServices(dockerStatusBuildItem, composeProjectBuildItem,
                    configuration.devservices(),
                    buildItemsConfig, elasticsearchConnectionBuildItems, launchMode, useSharedNetwork,
                    devServicesConfig.timeout());
            if (devDashboardService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devDashboardService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devDashboardService != null) {
                    shutdownElasticsearchDashboards();
                }
                first = true;
                devDashboardService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        if (devDashboardService.isOwner()) {
            log.infof(
                    "Dev Services for %s started. You can open th ui in your Browser at %s",
                    strategy.supportedDistribution().name(),
                    new DashboardDevServicesConfiguration(devDashboardService.getConfig()).hostName);
        }
        return devDashboardService.toBuildItem();
    }

    private RunningDevService startDashboardDevServices(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            List<DevservicesElasticsearchConnectionBuildItem> elasticsearchConnectionBuildItems,
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout) throws BuildException {
        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting Dashboard DevServices for Elasticsearch, as it has been disabled in the config.");
            return null;
        }

        if (!config.dashboard().enabled()) {
            // Kibana explicitly disabled
            log.debug("Not starting Kibana Dev Service, as it has been disabled in the config.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker is not working, cannot start the Kibana/OpenSearch dashboards dev service.");
            return null;
        }

        Set<String> backendHosts = determineHosts(buildItemConfig, elasticsearchConnectionBuildItems);

        if (hostsConfigAvailable(buildItemConfig)) {
            // Filter out all hosts that are not the same distribution as the supported distribution
            boolean distributionFromConfigSupported = resolveDistribution(config, buildItemConfig)
                    .equals(strategy.supportedDistribution());
            //We could pass quarkus.elasticsearch.username and quarkus.elasticsearch.password via DevservicesElasticsearchBuildItem if authentication is required.
            backendHosts.removeIf(host -> determineDistributionViaApi(host, null, null)
                    .map(distribution -> !distribution.equals(strategy.supportedDistribution()))
                    .orElse(!distributionFromConfigSupported));
        }
        if (backendHosts.isEmpty()) {
            log.infof("No hosts found for %s, skip starting the dev service.", strategy.supportedDistribution().name());
            return null;
        }

        DockerImageName resolvedImageName = resolveDashboardImageName(config, strategy.supportedDistribution());

        final Optional<ContainerAddress> maybeContainerAddress = dashboardContainerLocator.locateContainer(
                config.serviceName(),
                config.shared(),
                launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(resolvedImageName.getUnversionedPart(), "kibana", "opensearch-dashboards"),
                        DASHBOARD_PORT,
                        launchMode.getLaunchMode(), useSharedNetwork));

        // Starting the server
        final Supplier<RunningDevService> defaultDashboardsSupplier = () -> {

            String defaultNetworkId = composeProjectBuildItem.getDefaultNetworkId();
            Set<String> backendHostsLocahostReplaced = backendHosts.stream()
                    .map(host -> host.replace("localhost", "host.docker.internal")).collect(Collectors.toSet());
            CreatedContainer createdContainer = strategy.supportedDistribution().equals(Distribution.ELASTIC)
                    ? createKibanaContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork,
                            backendHostsLocahostReplaced)
                    : createDashboardsContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork,
                            backendHostsLocahostReplaced);
            GenericContainer<?> container = createdContainer.genericContainer();
            if (config.serviceName() != null) {
                container.withLabel(DEV_SERVICE_LABEL, config.serviceName());
                container.withLabel(Labels.QUARKUS_DEV_SERVICE, config.serviceName());
            }
            if (config.dashboard().port().isPresent()) {
                container.setPortBindings(List.of(config.dashboard().port().get() + ":" + DASHBOARD_PORT));
            }
            timeout.ifPresent(container::withStartupTimeout);
            container.withEnv(config.dashboard().containerEnv());
            container.withReuse(config.reuse());
            container.start();

            var httpHost = createdContainer.hostName + ":"
                    + (useSharedNetwork ? DASHBOARD_PORT : container.getMappedPort(DASHBOARD_PORT));
            return new RunningDevService(Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                    container.getContainerId(),
                    new ContainerShutdownCloseable(container, "Kibana"),
                    DashboardDevServicesConfiguration.buildPropertiesMap(strategy.supportedDistribution(), httpHost));
        };

        return maybeContainerAddress
                .map(containerAddress -> new RunningDevService(
                        Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                        containerAddress.getId(),
                        null,
                        DashboardDevServicesConfiguration.buildPropertiesMap(strategy.supportedDistribution(),
                                containerAddress.getUrl())))
                .orElseGet(defaultDashboardsSupplier);
    }

    private Set<String> determineHosts(DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            List<DevservicesElasticsearchConnectionBuildItem> elasticsearchConnectionBuildItems) {
        Set<String> hosts;
        if (hostsConfigAvailable(buildItemConfig)) {
            log.info("Elasticsearch hosts config property found, using it");
            hosts = buildItemConfig.hostsConfigProperties.stream().filter(ConfigUtils::isPropertyNonEmpty)
                    .flatMap(property -> ConfigProvider.getConfig().getValues(property, String.class).stream())
                    // We could pass quarkus.elasticsearch.protocol via DevservicesElasticsearchBuildItem if it is not http.
                    .map(host -> "http://" + host)
                    .collect(Collectors.toSet());
        } else {
            log.info(
                    "no Elasticsearch hosts config property found, using the host of the elasticsearch dev services container to connect");
            hosts = elasticsearchConnectionBuildItems.stream()
                    .filter(connection -> strategy.supportedDistribution().equals(connection.getDistribution()))
                    // We could pass quarkus.elasticsearch.protocol via DevservicesElasticsearchBuildItem if it is not http.
                    .map(connection -> ("http://" + connection.getHost() + ":" + connection.getPort()))
                    .collect(Collectors.toSet());
        }
        return hosts;
    }

    private boolean hostsConfigAvailable(DevservicesElasticsearchBuildItemsConfiguration buildItemConfig) {
        return buildItemConfig.hostsConfigProperties.stream().anyMatch(ConfigUtils::isPropertyNonEmpty);
    }

    private CreatedContainer createKibanaContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, String defaultNetworkId, boolean useSharedNetwork,
            Set<String> elasticsearchHosts) {
        //Create Generic Kibana container
        GenericContainer<?> container = new GenericContainer<>(
                resolvedImageName.asCompatibleSubstituteFor("docker.elastic.co/kibana/kibana"));

        String kibanaHostName = ConfigureUtil.configureNetwork(container, defaultNetworkId, useSharedNetwork,
                DEV_SERVICE_KIBANA);
        container.setExposedPorts(List.of(DASHBOARD_PORT));
        if (!elasticsearchHosts.isEmpty()) {
            container.addEnv("ELASTICSEARCH_HOSTS",
                    "[" + elasticsearchHosts.stream().map(url -> "\"" + url + "\"").collect(Collectors.joining(",")) + "]");
        }
        config.dashboard().nodeOpts().ifPresent(nodeOpts -> container.addEnv("NODE_OPTIONS", nodeOpts));
        return new CreatedContainer(container, kibanaHostName);
    }

    private CreatedContainer createDashboardsContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, String defaultNetworkId, boolean useSharedNetwork,
            Set<String> opensearchHosts) {
        //Create Generic Kibana container
        GenericContainer<?> container = new GenericContainer<>(
                resolvedImageName.asCompatibleSubstituteFor("opensearchproject/opensearch-dashboards"));

        String kibanaHostName = ConfigureUtil.configureNetwork(container, defaultNetworkId, useSharedNetwork,
                DEV_SERVICE_DASHBOARDS);
        container.setExposedPorts(List.of(DASHBOARD_PORT));
        if (!opensearchHosts.isEmpty()) {
            container.addEnv("OPENSEARCH_HOSTS",
                    "[" + opensearchHosts.stream().map(url -> "\"" + url + "\"").collect(Collectors.joining(",")) + "]");
        }

        config.dashboard().nodeOpts().ifPresent(nodeOpts -> container.addEnv("NODE_OPTIONS", nodeOpts));
        container.addEnv("DISABLE_SECURITY_DASHBOARDS_PLUGIN", "true");
        return new CreatedContainer(container, kibanaHostName);
    }

    private record CreatedContainer(GenericContainer<?> genericContainer, String hostName) {
    }

    static DockerImageName resolveDashboardImageName(ElasticsearchDevServicesBuildTimeConfig config,
            Distribution resolvedDistribution) {
        return DockerImageName.parse(config.dashboard().imageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor(
                Distribution.ELASTIC.equals(resolvedDistribution)
                        ? DEV_SERVICE_KIBANA
                        : DEV_SERVICE_DASHBOARDS)));
    }

    private void shutdownElasticsearchDashboards() {
        if (devDashboardService != null) {
            try {
                devDashboardService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Dashboards", e);
            } finally {
                devDashboardService = null;
            }
        }
    }

    private static class DashboardDevServicesConfiguration {
        private final Distribution distribution;
        private final String hostName;
        private final static String DISTRIBUTION_PROPERTY = "quarkus.elasticsearch.devservices.dashboard.distribution";
        private final static String HOST_NAME_PROPERTY = "quarkus.elasticsearch.devservices.dashboard.hostName";

        private DashboardDevServicesConfiguration(Distribution distribution, String hostName) {
            this.distribution = distribution;
            this.hostName = hostName;
        }

        private DashboardDevServicesConfiguration(Map<String, String> properties) {
            this.distribution = Distribution.valueOf(properties.get(DISTRIBUTION_PROPERTY));
            this.hostName = properties.get(HOST_NAME_PROPERTY);
        }

        private static Map<String, String> buildPropertiesMap(Distribution distribution, String hostName) {
            return Map.of(DISTRIBUTION_PROPERTY, distribution.name(), HOST_NAME_PROPERTY, hostName);
        }
    }

    public Optional<Distribution> determineDistributionViaApi(String url, String username, String password) {
        try {
            log.debugf("Call the API on %s to determine the distribution", url);
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes()));
            Map<String, Map<String, Object>> nodeInfo = objectMapper.readValue(connection.getInputStream(), Map.class);
            Map<String, Object> versionInfo = nodeInfo.get("version");
            if (versionInfo == null) {
                log.error("No version info found in the response");
                return Optional.empty();
            }
            Object distribution = versionInfo.get("distribution");
            if (distribution instanceof String) {
                log.debugf("The distribution is %s", ((String) distribution));
                try {
                    return Optional.of(Distribution.valueOf(((String) distribution).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.errorf("The distribution %s is not supported", ((String) distribution));
                    return Optional.empty();
                }
            } else {
                // Elastic does not return a distribution, so we assume it is Elastic
                return Optional.of(Distribution.ELASTIC);
            }
        } catch (IOException e) {
            log.error("Failed to determine the distribution via API", e);
            return Optional.empty();
        }
    }

}
