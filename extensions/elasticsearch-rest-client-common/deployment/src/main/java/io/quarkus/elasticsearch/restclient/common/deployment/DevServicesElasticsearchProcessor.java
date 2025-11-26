package io.quarkus.elasticsearch.restclient.common.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.DEV_SERVICE_ELASTICSEARCH;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.DEV_SERVICE_OPENSEARCH;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.buildPropertiesMap;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.getElasticsearchHosts;
import static io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchProcessorUtils.resolveDistribution;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
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
 * Starts an Elasticsearch server as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesElasticsearchProcessor {
    private static final Logger log = Logger.getLogger(DevServicesElasticsearchProcessor.class);

    /**
     * Label to add to shared Dev Service for Elasticsearch running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-elasticsearch";
    static final String NEW_DEV_SERVICE_LABEL = "io.quarkus.devservice.elasticsearch";
    static final int ELASTICSEARCH_PORT = 9200;

    private static final ContainerLocator elasticsearchContainerLocator = locateContainerWithLabels(ELASTICSEARCH_PORT,
            DEV_SERVICE_LABEL, NEW_DEV_SERVICE_LABEL);

    static volatile RunningDevService devService;

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
            DevServicesConfig devServicesConfig,
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
                (launchMode.isTest() ? "(test) " : "") + "Dev Services for Elasticsearch starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startElasticsearchDevServices(dockerStatusBuildItem, composeProjectBuildItem,
                    configuration.devservices(), buildItemsConfig, launchMode, useSharedNetwork, devServicesConfig.timeout());

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
                    getElasticsearchHosts(buildItemsConfig, devService));
        }
        return devService.toBuildItem();
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

    private RunningDevService startElasticsearchDevServices(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout) throws BuildException {
        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Elasticsearch, as it has been disabled in the config.");
            return null;
        }

        for (String hostsConfigProperty : buildItemConfig.hostsConfigProperties) {
            // Check if elasticsearch hosts property is set
            if (ConfigUtils.isPropertyNonEmpty(hostsConfigProperty)) {
                log.debugf("Not starting Dev Services for Elasticsearch, the %s property is configured.", hostsConfigProperty);
                return null;
            }
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warnf("Docker isn't working, please configure the Elasticsearch hosts property (%s).",
                    displayProperties(buildItemConfig.hostsConfigProperties));
            return null;
        }

        Distribution resolvedDistribution = resolveDistribution(config, buildItemConfig);
        DockerImageName resolvedImageName = resolveImageName(config, resolvedDistribution);

        final Optional<ContainerAddress> maybeContainerAddress = elasticsearchContainerLocator.locateContainer(
                config.serviceName(),
                config.shared(),
                launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(resolvedImageName.getUnversionedPart(), "elasticsearch", "opensearch"),
                        ELASTICSEARCH_PORT,
                        launchMode.getLaunchMode(), useSharedNetwork));

        // Starting the server
        final Supplier<RunningDevService> defaultElasticsearchSupplier = () -> {

            String defaultNetworkId = composeProjectBuildItem.getDefaultNetworkId();
            CreatedContainer createdContainer = resolvedDistribution.equals(Distribution.ELASTIC)
                    ? createElasticsearchContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork)
                    : createOpensearchContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork);
            GenericContainer<?> container = createdContainer.genericContainer();

            if (config.serviceName() != null) {
                container.withLabel(DEV_SERVICE_LABEL, config.serviceName());
                container.withLabel(Labels.QUARKUS_DEV_SERVICE, config.serviceName());
            }
            if (config.port().isPresent()) {
                container.setPortBindings(List.of(config.port().get() + ":" + ELASTICSEARCH_PORT));
            }
            timeout.ifPresent(container::withStartupTimeout);
            container.withEnv(config.containerEnv());
            container.withReuse(config.reuse());
            container.start();

            var httpHost = createdContainer.hostName + ":"
                    + (useSharedNetwork ? ELASTICSEARCH_PORT : container.getMappedPort(ELASTICSEARCH_PORT));
            return new RunningDevService(Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                    container.getContainerId(),
                    new ContainerShutdownCloseable(container, "Elasticsearch"),
                    buildPropertiesMap(buildItemConfig, httpHost));
        };

        return maybeContainerAddress
                .map(containerAddress -> new RunningDevService(
                        Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                        containerAddress.getId(),
                        null,
                        buildPropertiesMap(buildItemConfig, containerAddress.getUrl())))
                .orElseGet(defaultElasticsearchSupplier);
    }

    private CreatedContainer createElasticsearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, String defaultNetworkId, boolean useSharedNetwork) {
        ElasticsearchContainer container = new ElasticsearchContainer(
                resolvedImageName.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"));
        String hostName = ConfigureUtil.configureNetwork(container, defaultNetworkId, useSharedNetwork,
                DEV_SERVICE_ELASTICSEARCH);

        // Disable security as else we would need to configure it correctly to avoid tons of WARNING in the log
        container.addEnv("xpack.security.enabled", "false");
        // disable enrollment token to allow Kibana in a non-interactive automated way
        container.addEnv("xpack.security.enrollment.enabled", "false");
        container.addEnv("discovery.type", "single-node");
        // Disable disk-based shard allocation thresholds:
        // in a single-node setup they just don't make sense,
        // and lead to problems on large disks with little space left.
        // See https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-cluster.html#disk-based-shard-allocation
        container.addEnv("cluster.routing.allocation.disk.threshold_enabled", "false");
        container.addEnv("ES_JAVA_OPTS", config.javaOpts());

        return new CreatedContainer(container, hostName);
    }

    private CreatedContainer createOpensearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, String defaultNetworkId, boolean useSharedNetwork) {
        OpensearchContainer<?> container = new OpensearchContainer<>(
                resolvedImageName.asCompatibleSubstituteFor("opensearchproject/opensearch"));
        String hostName = ConfigureUtil.configureNetwork(container, defaultNetworkId, useSharedNetwork, DEV_SERVICE_OPENSEARCH);

        container.addEnv("bootstrap.memory_lock", "true");
        container.addEnv("plugins.index_state_management.enabled", "false");
        // Disable disk-based shard allocation thresholds: on large, relatively full disks (>90% used),
        // it will lead to index creation to get stuck waiting for other nodes to join the cluster,
        // which will never happen since we only have one node.
        // See https://opensearch.org/docs/latest/api-reference/cluster-api/cluster-settings/
        container.addEnv("cluster.routing.allocation.disk.threshold_enabled", "false");
        container.addEnv("OPENSEARCH_JAVA_OPTS", config.javaOpts());
        // OpenSearch 2.12 and later requires an admin password, or it won't start.
        // Considering dev services are transient and not intended for production by nature,
        // we'll just set some hardcoded password.
        container.addEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "NotActua11y$trongPa$$word");

        return new CreatedContainer(container, hostName);
    }

    private record CreatedContainer(GenericContainer<?> genericContainer, String hostName) {
    }

    private DockerImageName resolveImageName(ElasticsearchDevServicesBuildTimeConfig config,
            Distribution resolvedDistribution) {
        return DockerImageName.parse(config.imageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor(
                Distribution.ELASTIC.equals(resolvedDistribution)
                        ? DEV_SERVICE_ELASTICSEARCH
                        : DEV_SERVICE_OPENSEARCH)));
    }

    private String displayProperties(Set<String> hostsConfigProperties) {
        return String.join(" and ", hostsConfigProperties);
    }
}
