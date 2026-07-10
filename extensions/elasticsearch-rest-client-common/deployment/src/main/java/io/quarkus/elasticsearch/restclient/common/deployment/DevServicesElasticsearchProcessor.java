package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.Container;

import io.quarkus.builder.BuildException;
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
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.StartableContainer;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts Elasticsearch servers as dev services if needed.
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
    static final String DEV_SERVICE_DISTRIBUTION_LABEL = "io.quarkus.devservice.elasticsearch.distribution";
    static final int ELASTICSEARCH_PORT = 9200;

    private static final String[] DEV_SERVICE_LABELS = { DEV_SERVICE_LABEL, NEW_DEV_SERVICE_LABEL };

    private static final ContainerLocator ELASTICSEARCH_CONTAINER_LOCATOR = containerLocatorForDistribution(
            Distribution.ELASTIC);
    private static final ContainerLocator OPENSEARCH_CONTAINER_LOCATOR = containerLocatorForDistribution(
            Distribution.OPENSEARCH);

    private static final Distribution DEFAULT_DISTRIBUTION = Distribution.ELASTIC;
    private static final String DEV_SERVICE_ELASTICSEARCH = "elasticsearch";
    private static final String DEV_SERVICE_OPENSEARCH = "opensearch";

    @BuildStep
    public void startElasticsearchDevServices(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            ElasticsearchCommonBuildTimeConfig configuration,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesConfig devServicesConfig,
            List<DevservicesElasticsearchBuildItem> devservicesElasticsearchBuildItems,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) throws BuildException {

        if (devservicesElasticsearchBuildItems.isEmpty()) {
            // safety belt in case a module depends on this one without producing the build item
            return;
        }

        Collection<DevservicesElasticsearchBuildItemsConfiguration> servicesToStart = groupBuildItemsByClient(
                devservicesElasticsearchBuildItems);

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        for (DevservicesElasticsearchBuildItemsConfiguration buildItemsConfig : servicesToStart) {
            ElasticsearchDevServicesBuildTimeConfig config = configuration.clients()
                    .get(buildItemsConfig.clientName)
                    .devservices();

            DevServicesResultBuildItem devService = startElasticsearchDevService(
                    dockerStatusBuildItem, composeProjectBuildItem, config, buildItemsConfig,
                    launchMode, useSharedNetwork);
            if (devService != null) {
                devServicesResult.produce(devService);
            }
        }
    }

    private DevServicesResultBuildItem startElasticsearchDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemsConfig,
            LaunchModeBuildItem launchMode,
            boolean useSharedNetwork) throws BuildException {

        if (!config.enabled().orElse(true)) {
            log.debugf("Not starting [%s] Dev Service for Elasticsearch, as it has been disabled in the config.",
                    buildItemsConfig.clientName);
            return null;
        }

        for (String hostsConfigProperty : buildItemsConfig.hostsConfigProperties) {
            if (ConfigUtils.isPropertyNonEmpty(hostsConfigProperty)) {
                log.debugf("Not starting [%s] Dev Service for Elasticsearch, the %s property is configured.",
                        buildItemsConfig.clientName, hostsConfigProperty);
                return null;
            }
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warnf("Docker isn't working, please configure the Elasticsearch hosts %s instead: %s.",
                    buildItemsConfig.hostsConfigProperties.size() == 1 ? "property" : "properties",
                    displayProperties(buildItemsConfig.hostsConfigProperties));
            return null;
        }

        Distribution resolvedDistribution = resolveDistribution(config, buildItemsConfig);
        DockerImageName resolvedImageName = resolveImageName(config, resolvedDistribution);

        DevServicesResultBuildItem discovered = discoverRunningService(composeProjectBuildItem, config,
                resolvedDistribution, resolvedImageName, buildItemsConfig, launchMode.getLaunchMode(), useSharedNetwork);
        if (discovered != null) {
            return discovered;
        }

        return DevServicesResultBuildItem.owned()
                .feature(Feature.ELASTICSEARCH_REST_CLIENT_COMMON)
                .serviceName(buildItemsConfig.clientName)
                .serviceConfig(config)
                .startable(() -> {
                    String defaultNetworkId = composeProjectBuildItem.getDefaultNetworkId();
                    CreatedContainer createdContainer = resolvedDistribution.equals(Distribution.ELASTIC)
                            ? createElasticsearchContainer(config, resolvedImageName, defaultNetworkId,
                                    useSharedNetwork)
                            : createOpensearchContainer(config, resolvedImageName, defaultNetworkId,
                                    useSharedNetwork);
                    GenericContainer<?> container = createdContainer.genericContainer();

                    container.withLabel(DEV_SERVICE_LABEL, config.serviceName());
                    container.withLabel(Labels.QUARKUS_DEV_SERVICE, config.serviceName());
                    container.withLabel(DEV_SERVICE_DISTRIBUTION_LABEL,
                            resolvedDistribution.name().toLowerCase(Locale.ROOT));
                    if (config.port().isPresent()) {
                        container.setPortBindings(
                                List.of(config.port().get() + ":" + ELASTICSEARCH_PORT));
                    }

                    container.withEnv(config.containerEnv());
                    container.withReuse(config.reuse());

                    return new StartableContainer<>(container,
                            c -> createdContainer.hostName() + ":"
                                    + (useSharedNetwork ? ELASTICSEARCH_PORT
                                            : c.getMappedPort(ELASTICSEARCH_PORT)));
                })
                .postStartHook(containerWrapper -> log.infof(
                        "Dev Services for Elasticsearch [%s] started. Other Quarkus applications in dev mode will find the "
                                + "server automatically. For Quarkus applications in production mode, you can connect to"
                                + " this by configuring your application to use %s",
                        buildItemsConfig.clientName, containerWrapper.getConnectionInfo()))
                .configProvider(buildConfigProviderMap(buildItemsConfig))
                .build();
    }

    private DevServicesResultBuildItem discoverRunningService(
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ElasticsearchDevServicesBuildTimeConfig config,
            Distribution resolvedDistribution,
            DockerImageName resolvedImageName,
            DevservicesElasticsearchBuildItemsConfiguration buildItemsConfig,
            LaunchMode launchMode,
            boolean useSharedNetwork) {
        ContainerLocator locator = Distribution.OPENSEARCH.equals(resolvedDistribution)
                ? OPENSEARCH_CONTAINER_LOCATOR
                : ELASTICSEARCH_CONTAINER_LOCATOR;
        String distributionKeyword = Distribution.OPENSEARCH.equals(resolvedDistribution)
                ? DEV_SERVICE_OPENSEARCH
                : DEV_SERVICE_ELASTICSEARCH;
        return locator
                .locateContainer(config.serviceName(), config.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(resolvedImageName.getUnversionedPart(), distributionKeyword),
                        ELASTICSEARCH_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> DevServicesResultBuildItem.discovered()
                        .feature(Feature.ELASTICSEARCH_REST_CLIENT_COMMON)
                        .containerId(containerAddress.getId())
                        .config(buildPropertiesMap(buildItemsConfig, containerAddress.getUrl()))
                        .build())
                .orElse(null);
    }

    private static ContainerLocator containerLocatorForDistribution(Distribution distribution) {
        String expectedDistribution = distribution.name().toLowerCase(Locale.ROOT);
        return new ContainerLocator(
                (container, expectedLabel) -> hasMatchingDistribution(container, expectedDistribution)
                        && hasDevServiceLabels(container, expectedLabel),
                ELASTICSEARCH_PORT);
    }

    private static boolean hasMatchingDistribution(Container container, String expectedDistribution) {
        String label = container.getLabels().get(DEV_SERVICE_DISTRIBUTION_LABEL);
        return expectedDistribution.equals(label);
    }

    private static boolean hasDevServiceLabels(Container container, String expectedLabel) {
        return Stream.concat(Stream.of(Labels.QUARKUS_DEV_SERVICE), Arrays.stream(DEV_SERVICE_LABELS))
                .map(l -> container.getLabels().get(l))
                .anyMatch(expectedLabel::equals);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map buildConfigProviderMap(
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig) {
        Map<String, Function<StartableContainer<?>, String>> map = new HashMap<>();
        for (String property : buildItemConfig.hostsConfigProperties) {
            map.put(property, StartableContainer::getConnectionInfo);
        }
        return map;
    }

    private CreatedContainer createElasticsearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
            DockerImageName resolvedImageName, String defaultNetworkId, boolean useSharedNetwork) {
        ElasticsearchContainer container = new ElasticsearchContainer(
                resolvedImageName.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"));
        String hostName = ConfigureUtil.configureNetwork(container, defaultNetworkId, useSharedNetwork,
                DEV_SERVICE_ELASTICSEARCH);

        // Disable security as else we would need to configure it correctly to avoid tons of WARNING in the log
        container.addEnv("xpack.security.enabled", "false");
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
        OpensearchContainer container = new OpensearchContainer(
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

    private Distribution resolveDistribution(ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig) throws BuildException {
        // First, let's see if it was explicitly configured:
        if (config.distribution().isPresent()) {
            return config.distribution().get();
        }
        // Now let's see if we can guess it from the image:
        if (config.imageName().isPresent()) {
            String imageNameRepository = DockerImageName.parse(config.imageName().get()).getRepository()
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
                            + config.imageName().get()
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

    private static String displayProperties(Set<String> hostsConfigProperties) {
        return String.join(" and ", hostsConfigProperties);
    }

    private static Collection<DevservicesElasticsearchBuildItemsConfiguration> groupBuildItemsByClient(
            List<DevservicesElasticsearchBuildItem> devservicesElasticsearchBuildItems) throws BuildException {
        Map<String, DevservicesElasticsearchBuildItemsConfiguration> result = new HashMap<>();
        for (DevservicesElasticsearchBuildItem buildItem : devservicesElasticsearchBuildItems) {
            DevservicesElasticsearchBuildItemsConfiguration existing = result.get(buildItem.getName());
            if (existing == null) {
                existing = new DevservicesElasticsearchBuildItemsConfiguration(buildItem);
                result.put(buildItem.getName(), existing);
            } else {
                if (existing.version == null) {
                    existing.version = buildItem.getVersion();
                } else if (buildItem.getVersion() != null && !existing.version.equals(buildItem.getVersion())) {
                    throw new BuildException(
                            "Multiple extensions request different versions of Elasticsearch for Dev Services.",
                            Collections.emptyList());
                }

                if (existing.distribution == null) {
                    existing.distribution = buildItem.getDistribution();
                } else if (buildItem.getDistribution() != null
                        && !existing.distribution.equals(buildItem.getDistribution())) {
                    throw new BuildException(
                            "Multiple extensions request different distributions of Elasticsearch for Dev Services.",
                            Collections.emptyList());
                }
                existing.hostsConfigProperties.add(buildItem.getHostsConfigProperty());
            }
        }
        return result.values();
    }

    private static class DevservicesElasticsearchBuildItemsConfiguration {
        private final String clientName;
        private String version;
        private Distribution distribution;
        private final Set<String> hostsConfigProperties;

        private DevservicesElasticsearchBuildItemsConfiguration(DevservicesElasticsearchBuildItem buildItem) {
            this.clientName = buildItem.getName();
            this.version = buildItem.getVersion();
            this.distribution = buildItem.getDistribution();
            this.hostsConfigProperties = new HashSet<>();
            this.hostsConfigProperties.add(buildItem.getHostsConfigProperty());
        }
    }
}
