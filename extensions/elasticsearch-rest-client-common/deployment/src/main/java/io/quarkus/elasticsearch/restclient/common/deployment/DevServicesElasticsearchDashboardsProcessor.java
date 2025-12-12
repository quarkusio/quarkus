package io.quarkus.elasticsearch.restclient.common.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.DEV_SERVICE_LABEL;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.ELASTICSEARCH_PORT;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.NEW_DEV_SERVICE_LABEL;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.buildPropertiesMap;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.getElasticsearchHosts;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.resolveDistribution;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
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

/**
 * Starts an Elasticsearch Kibana/OpenSearch Dashboards as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesElasticsearchDashboardsProcessor {
    private static final Logger log = Logger.getLogger(DevServicesElasticsearchDashboardsProcessor.class);
    static final int DASHBOARD_PORT = 5601;
    private static final String DEV_SERVICE_KIBANA = "elasticsearch-kibana";
    private static final String DEV_SERVICE_DASHBOARDS = "opensearch-dashboards";
    private static final String DEV_SERVICE_LABEL = "io.quarkus.devservice.elasticsearch.dashboards";
    private static final ContainerLocator dashboardContainerLocator = locateContainerWithLabels(DASHBOARD_PORT, DEV_SERVICE_LABEL);
    static volatile RunningDevService devDashboardService;
    static volatile ElasticsearchCommonBuildTimeConfig cfg;
    static volatile boolean first = true;

    @BuildStep
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
            DevServicesConfig devServicesConfig) throws BuildException {

        if (elasticsearchConnectionBuildItems.isEmpty()) {
            // safety belt in case a module depends on this one without producing the build item
            return null;
        }

        // TODO: group the `elasticsearchConnectionBuildItems` so that we know what distributions to start and what hosts to pass to each.

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
                    "Dev Services for Elasticsearch Dashboards started. You can open th ui in your Browser at %s",
                    getElasticsearchHosts(buildItemsConfig, devDashboardService));
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

        Distribution resolvedDistribution = resolveDistribution(config, buildItemConfig);
        DockerImageName resolvedImageName = resolveDashboardImageName(config, resolvedDistribution);

        Set<String> opensearchHosts;
        if (buildItemConfig.hostsConfigProperties.stream().anyMatch(ConfigUtils::isPropertyNonEmpty)) {
            log.error("elasticsearch hosts config property found, using it");
            opensearchHosts = buildItemConfig.hostsConfigProperties.stream().filter(ConfigUtils::isPropertyNonEmpty)
                    .flatMap(property -> ConfigProvider.getConfig().getValues(property, String.class).stream())
                    .map(host -> "http://" + host.replace("localhost", "host.docker.internal"))
                    .collect(Collectors.toSet());
        } else {
            Optional<ContainerAddress> maybeContainerAddressSearchBackend = Optional.empty();
            log.info(
                    "no elasticsearch hosts config property found, using the host of theelasticsearch dev services container to connect");

            opensearchHosts = elasticsearchConnectionBuildItems.stream()
                    .map(connection -> ("http://" + connection.getHost() + ":" + connection.getPort())
                            .replace("localhost", "host.docker.internal"))
                    .collect(Collectors.toSet());
        }

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
            CreatedContainer createdContainer = resolvedDistribution.equals(Distribution.ELASTIC)
                    ? createKibanaContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork, launchMode,
                            composeProjectBuildItem, opensearchHosts)
                    : createDashboardsContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork, launchMode,
                            composeProjectBuildItem, opensearchHosts);
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
                    buildPropertiesMap(buildItemConfig, httpHost));
        };

        return maybeContainerAddress
                .map(containerAddress -> new RunningDevService(
                        Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                        containerAddress.getId(),
                        null,
                        buildPropertiesMap(buildItemConfig, containerAddress.getUrl())))
                .orElseGet(defaultDashboardsSupplier);
    }

    private CreatedContainer createKibanaContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, String defaultNetworkId, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode, DevServicesComposeProjectBuildItem composeProjectBuildItem,
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
            LaunchModeBuildItem launchMode, DevServicesComposeProjectBuildItem composeProjectBuildItem,
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
        return DockerImageName.parse(config.imageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor(
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

}
