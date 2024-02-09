package io.quarkus.elasticsearch.restclient.common.deployment;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
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
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchDevServicesBuildTimeConfig.Distribution;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts an Elasticsearch server as dev service if needed.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
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
    private static final Distribution DEFAULT_DISTRIBUTION = Distribution.ELASTIC;
    private static final String DEV_SERVICE_ELASTICSEARCH = "elasticsearch";
    private static final String DEV_SERVICE_OPENSEARCH = "opensearch";

    static volatile DevServicesResultBuildItem.RunningDevService devService;
    static volatile ElasticsearchDevServicesBuildTimeConfig cfg;
    static volatile boolean first = true;

    @BuildStep
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
                (launchMode.isTest() ? "(test) " : "") + "Dev Services for Elasticsearch starting:",
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
            log.debug("Not starting Dev Services for Elasticsearch, as it has been disabled in the config.");
            return null;
        }

        for (String hostsConfigProperty : buildItemConfig.hostsConfigProperties) {
            // Check if elasticsearch hosts property is set
            if (ConfigUtils.isPropertyPresent(hostsConfigProperty)) {
                log.debugf("Not starting Dev Services for Elasticsearch, the %s property is configured.", hostsConfigProperty);
                return null;
            }
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warnf("Docker isn't working, please configure the Elasticsearch hosts property (%s).",
                    displayProperties(buildItemConfig.hostsConfigProperties));
            return null;
        }

        Distribution resolvedDistribution = resolveDistribution(config, buildItemConfig);
        DockerImageName resolvedImageName = resolveImageName(config, resolvedDistribution);
        // Hibernate Search Elasticsearch have a version configuration property, we need to check that it is coherent
        // with the image we are about to launch
        if (buildItemConfig.version != null) {
            String containerTag = resolvedImageName.getVersionPart();
            if (!containerTag.startsWith(buildItemConfig.version)) {
                throw new BuildException(
                        "Dev Services for Elasticsearch detected a version mismatch."
                                + " Consuming extensions are configured to use version " + config.imageName
                                + " but Dev Services are configured to use version " + buildItemConfig.version +
                                ". Either configure the same version or disable Dev Services for Elasticsearch.",
                        Collections.emptyList());
            }
        }

        if (buildItemConfig.distribution != null
                && !buildItemConfig.distribution.equals(resolvedDistribution)) {
            throw new BuildException(
                    "Dev Services for Elasticsearch detected a distribution mismatch."
                            + " Consuming extensions are configured to use distribution " + config.distribution
                            + " but Dev Services are configured to use distribution " + buildItemConfig.distribution +
                            ". Either configure the same distribution or disable Dev Services for Elasticsearch.",
                    Collections.emptyList());
        }

        final Optional<ContainerAddress> maybeContainerAddress = elasticsearchContainerLocator.locateContainer(
                config.serviceName,
                config.shared,
                launchMode.getLaunchMode());

        // Starting the server
        final Supplier<DevServicesResultBuildItem.RunningDevService> defaultElasticsearchSupplier = () -> {

            GenericContainer<?> container = resolvedDistribution.equals(Distribution.ELASTIC)
                    ? createElasticsearchContainer(config, resolvedImageName, useSharedNetwork)
                    : createOpensearchContainer(config, resolvedImageName, useSharedNetwork);

            if (config.serviceName != null) {
                container.withLabel(DEV_SERVICE_LABEL, config.serviceName);
            }
            if (config.port.isPresent()) {
                container.setPortBindings(List.of(config.port.get() + ":" + ELASTICSEARCH_PORT));
            }
            timeout.ifPresent(container::withStartupTimeout);

            container.withEnv(config.containerEnv);

            container.withReuse(config.reuse);

            container.start();
            return new DevServicesResultBuildItem.RunningDevService(Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                    container.getContainerId(),
                    new ContainerShutdownCloseable(container, "Elasticsearch"),
                    buildPropertiesMap(buildItemConfig,
                            container.getHost() + ":" + container.getMappedPort(ELASTICSEARCH_PORT)));
        };

        return maybeContainerAddress
                .map(containerAddress -> new DevServicesResultBuildItem.RunningDevService(
                        Feature.ELASTICSEARCH_REST_CLIENT_COMMON.getName(),
                        containerAddress.getId(),
                        null,
                        buildPropertiesMap(buildItemConfig, containerAddress.getUrl())))
                .orElseGet(defaultElasticsearchSupplier);
    }

    private GenericContainer<?> createElasticsearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, boolean useSharedNetwork) {
        ElasticsearchContainer container = new ElasticsearchContainer(
                resolvedImageName.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"));
        if (useSharedNetwork) {
            ConfigureUtil.configureSharedNetwork(container, DEV_SERVICE_ELASTICSEARCH);
        }

        // Disable security as else we would need to configure it correctly to avoid tons of WARNING in the log
        container.addEnv("xpack.security.enabled", "false");
        // Disable disk-based shard allocation thresholds:
        // in a single-node setup they just don't make sense,
        // and lead to problems on large disks with little space left.
        // See https://www.elastic.co/guide/en/elasticsearch/reference/8.9/modules-cluster.html#disk-based-shard-allocation
        container.addEnv("cluster.routing.allocation.disk.threshold_enabled", "false");
        container.addEnv("ES_JAVA_OPTS", config.javaOpts);
        return container;
    }

    private GenericContainer<?> createOpensearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, boolean useSharedNetwork) {
        OpensearchContainer container = new OpensearchContainer(
                resolvedImageName.asCompatibleSubstituteFor("opensearchproject/opensearch"));
        if (useSharedNetwork) {
            ConfigureUtil.configureSharedNetwork(container, DEV_SERVICE_OPENSEARCH);
        }

        container.addEnv("bootstrap.memory_lock", "true");
        container.addEnv("plugins.index_state_management.enabled", "false");
        // Disable disk-based shard allocation thresholds: on large, relatively full disks (>90% used),
        // it will lead to index creation to get stuck waiting for other nodes to join the cluster,
        // which will never happen since we only have one node.
        // See https://opensearch.org/docs/latest/api-reference/cluster-api/cluster-settings/
        container.addEnv("cluster.routing.allocation.disk.threshold_enabled", "false");
        container.addEnv("OPENSEARCH_JAVA_OPTS", config.javaOpts);
        return container;
    }

    private DockerImageName resolveImageName(ElasticsearchDevServicesBuildTimeConfig config,
            Distribution resolvedDistribution) {
        return DockerImageName.parse(config.imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor(
                Distribution.ELASTIC.equals(resolvedDistribution)
                        ? DEV_SERVICE_ELASTICSEARCH
                        : DEV_SERVICE_OPENSEARCH)));
    }

    private Distribution resolveDistribution(ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig) throws BuildException {
        // First, let's see if it was explicitly configured:
        if (config.distribution.isPresent()) {
            return config.distribution.get();
        }
        // Now let's see if we can guess it from the image:
        if (config.imageName.isPresent()) {
            String imageNameRepository = DockerImageName.parse(config.imageName.get()).getRepository()
                    .toLowerCase(Locale.ROOT);
            if (imageNameRepository.contains(DEV_SERVICE_OPENSEARCH)) {
                return Distribution.OPENSEARCH;
            }
            if (imageNameRepository.contains(DEV_SERVICE_ELASTICSEARCH)) {
                return Distribution.ELASTIC;
            }
            // no luck guessing so let's ask user to be more explicit:
            throw new BuildException(
                    "Wasn't able to determine the distribution of the search service based on the provided image name ["
                            + config.imageName.get()
                            + "]. Please specify the distribution explicitly.",
                    Collections.emptyList());
        }
        // Otherwise, let's see if the build item has a value available:
        if (buildItemConfig.distribution != null) {
            return buildItemConfig.distribution;
        }
        // If we didn't get an explicit distribution
        // and no image name was provided
        // then elastic is a default distribution:
        return DEFAULT_DISTRIBUTION;
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
        private Distribution distribution;

        private DevservicesElasticsearchBuildItemsConfiguration(List<DevservicesElasticsearchBuildItem> buildItems)
                throws BuildException {
            hostsConfigProperties = new HashSet<>(buildItems.size());

            // check that all build items agree on the version and distribution to start
            for (DevservicesElasticsearchBuildItem buildItem : buildItems) {
                if (version == null) {
                    version = buildItem.getVersion();
                } else if (!version.equals(buildItem.getVersion())) {
                    // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the version
                    throw new BuildException(
                            "Multiple extensions request different versions of Elasticsearch for Dev Services.",
                            Collections.emptyList());
                }

                if (distribution == null) {
                    distribution = buildItem.getDistribution();
                } else if (!distribution.equals(buildItem.getDistribution())) {
                    // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the distribution
                    throw new BuildException(
                            "Multiple extensions request different distributions of Elasticsearch for Dev Services.",
                            Collections.emptyList());
                }

                hostsConfigProperties.add(buildItem.getHostsConfigProperty());
            }
        }
    }
}
