package io.quarkus.elasticsearch.restclient.common.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
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

    private static final Distribution DEFAULT_DISTRIBUTION = Distribution.ELASTIC;
    private static final String DEV_SERVICE_ELASTICSEARCH = "elasticsearch";
    private static final String DEV_SERVICE_OPENSEARCH = "opensearch";

    @BuildStep
    public void startElasticsearchDevServices(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DevServicesConfig devServicesConfig,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            ElasticsearchCommonBuildTimeConfig clientConfiguration,
            List<DevservicesElasticsearchBuildItem> devservicesElasticsearchBuildItems,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            BuildProducer<DevServicesResultBuildItem> devServicesResultBuildItemBuildProducer) throws BuildException {

        if (devservicesElasticsearchBuildItems.isEmpty()) {
            // safety belt in case a module depends on this one without producing the build item
            return;
        }

        Collection<DevservicesElasticsearchBuildItemsConfiguration> servicesToStart = DevservicesElasticsearchBuildItemsConfiguration
                .convert(devservicesElasticsearchBuildItems);

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        for (DevservicesElasticsearchBuildItemsConfiguration elasticsearch : servicesToStart) {
            ElasticsearchDevServicesBuildTimeConfig devServicesBuildTimeConfig = clientConfiguration.devservices()
                    .get(elasticsearch.clientName);
            DevServicesResultBuildItem devService = startElasticsearchDevService(dockerStatusBuildItem, composeProjectBuildItem,
                    devServicesBuildTimeConfig, elasticsearch, launchMode, useSharedNetwork, devServicesConfig.timeout());
            if (devService == null) {
                continue;
            }
            devServicesResultBuildItemBuildProducer.produce(devService);
        }
    }

    private Map<String, String> buildMapConfig(DevservicesElasticsearchBuildItemsConfiguration configuration,
            ElasticsearchDevServicesBuildTimeConfig clientConfig) {
        Map<String, String> result = new HashMap<>();

        String clientName = configuration.clientName;
        if (clientConfig == null) {
            result.put(clientName + ".distribution", Objects.toString(configuration.distribution));
            result.put(clientName + ".version", configuration.version);
        } else {
            result.put(clientName + ".enabled", Objects.toString(clientConfig.enabled().orElse(true)));
            result.put(clientName + ".port", Objects.toString(clientConfig.port().orElse(-1)));
            result.put(clientName + ".distribution",
                    Objects.toString(clientConfig.distribution().orElse(Distribution.ELASTIC)));
            clientConfig.imageName().ifPresent(name -> result.put(clientName + ".imageName", name));
            result.put(clientName + ".javaOpts", clientConfig.javaOpts());
            result.put(clientName + ".shared", Objects.toString(clientConfig.shared()));
            result.put(clientName + ".serviceName", clientConfig.serviceName());
            result.put(clientName + ".containerEnv", clientConfig.containerEnv().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(";")));
            result.put(clientName + ".reuse", Objects.toString(clientConfig.reuse()));
        }

        return result;
    }

    private DevServicesResultBuildItem startElasticsearchDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ElasticsearchDevServicesBuildTimeConfig config,
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            LaunchModeBuildItem launchMode,
            boolean useSharedNetwork,
            Optional<Duration> timeout) throws BuildException {
        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debugf("Not starting [%s] Dev Service for Elasticsearch, as it has been disabled in the config.",
                    buildItemConfig.clientName);
            return null;
        }

        for (String hostsConfigProperty : buildItemConfig.hostsConfigProperties) {
            // Check if elasticsearch hosts property is set
            if (ConfigUtils.isPropertyNonEmpty(hostsConfigProperty)) {
                log.debugf("Not starting [%s] Dev Service for Elasticsearch, the %s property is configured.",
                        buildItemConfig.clientName, hostsConfigProperty);
                return null;
            }
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warnf("Docker isn't working, please configure the Elasticsearch hosts %s instead: %s.",
                    buildItemConfig.hostsConfigProperties.size() == 1 ? "property" : "properties",
                    displayProperties(buildItemConfig.hostsConfigProperties));
            return null;
        }

        Distribution resolvedDistribution = resolveDistribution(config, buildItemConfig);
        DockerImageName resolvedImageName = resolveImageName(config, resolvedDistribution);

        final Optional<ContainerAddress> maybeContainerAddress = elasticsearchContainerLocator.locateContainer(
                config.serviceName(),
                config.shared(),
                launchMode.getLaunchMode(),
                containerWithMappedPublicPort(config.port()))
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(resolvedImageName.getUnversionedPart(), "elasticsearch", "opensearch"),
                        ELASTICSEARCH_PORT,
                        launchMode.getLaunchMode(), useSharedNetwork));

        // Starting the server
        final Supplier<DevServicesResultBuildItem> defaultElasticsearchSupplier = () -> {

            String defaultNetworkId = composeProjectBuildItem.getDefaultNetworkId();
            QuarkusElasticsearchContainer quarkusElasticsearchContainer = resolvedDistribution.equals(Distribution.ELASTIC)
                    ? createElasticsearchContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork)
                    : createOpensearchContainer(config, resolvedImageName, defaultNetworkId, useSharedNetwork);
            GenericContainer<?> container = quarkusElasticsearchContainer.genericContainer();

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

            return DevServicesResultBuildItem.owned()
                    .feature(Feature.ELASTICSEARCH_REST_CLIENT_COMMON)
                    .serviceName(buildItemConfig.clientName)
                    .serviceConfig(config)
                    .configProvider(hostConfigProvider(buildItemConfig))
                    .startable(() -> quarkusElasticsearchContainer)
                    .build();
        };

        return maybeContainerAddress
                .filter(containerWithMappedPublicPort(config.port()))
                .map(containerAddress -> DevServicesResultBuildItem.discovered()
                        .feature(Feature.ELASTICSEARCH_REST_CLIENT_COMMON)
                        .config(hostConfig(buildItemConfig, containerAddress.getUrl()))
                        .build())
                .orElseGet(defaultElasticsearchSupplier);
    }

    private static Predicate<ContainerAddress> containerWithMappedPublicPort(Optional<Integer> port) {
        return containerAddress -> port.map(p -> p.equals(containerAddress.getPort())).orElse(true);
    }

    private QuarkusElasticsearchContainer createElasticsearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
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

        return new QuarkusElasticsearchContainer(container, hostName, useSharedNetwork);
    }

    private QuarkusElasticsearchContainer createOpensearchContainer(ElasticsearchDevServicesBuildTimeConfig config,
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

        return new QuarkusElasticsearchContainer(container, hostName, useSharedNetwork);
    }

    private record QuarkusElasticsearchContainer(GenericContainer<?> genericContainer, String hostName,
            boolean useSharedNetwork) implements Startable {
        @Override
        public void start() {
            genericContainer.start();
        }

        @Override
        public String getConnectionInfo() {
            return hostName + ":"
                    + (useSharedNetwork ? ELASTICSEARCH_PORT : genericContainer.getMappedPort(ELASTICSEARCH_PORT));
        }

        @Override
        public String getContainerId() {
            return genericContainer.getContainerId();
        }

        @Override
        public void close() {
            genericContainer.close();
        }
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

    private Map<String, Function<Startable, String>> hostConfigProvider(
            DevservicesElasticsearchBuildItemsConfiguration buildItemConfig) {
        Map<String, Function<Startable, String>> propertiesToSet = new HashMap<>();
        for (String property : buildItemConfig.hostsConfigProperties) {
            propertiesToSet.put(property, Startable::getConnectionInfo);
        }
        return propertiesToSet;
    }

    private Map<String, String> hostConfig(DevservicesElasticsearchBuildItemsConfiguration buildItemConfig, String host) {
        Map<String, String> propertiesToSet = new HashMap<>();
        for (String property : buildItemConfig.hostsConfigProperties) {
            propertiesToSet.put(property, host);
        }
        return propertiesToSet;
    }

    private String displayProperties(Set<String> hostsConfigProperties) {
        return String.join(" and ", hostsConfigProperties);
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

        static Collection<DevservicesElasticsearchBuildItemsConfiguration> convert(
                List<DevservicesElasticsearchBuildItem> buildItems) throws BuildException {
            Map<String, DevservicesElasticsearchBuildItemsConfiguration> result = new HashMap<>();
            for (DevservicesElasticsearchBuildItem buildItem : buildItems) {
                DevservicesElasticsearchBuildItemsConfiguration existingItem = result.get(buildItem.getName());
                if (existingItem == null) {
                    existingItem = new DevservicesElasticsearchBuildItemsConfiguration(buildItem);
                    result.put(buildItem.getName(), existingItem);
                } else {
                    if (existingItem.version == null) {
                        existingItem.version = buildItem.getVersion();
                    } else if (buildItem.getVersion() != null && !existingItem.version.equals(buildItem.getVersion())) {
                        // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the version
                        throw new BuildException(
                                "Multiple extensions request different versions of Elasticsearch for Dev Services.",
                                Collections.emptyList());
                    }

                    if (existingItem.distribution == null) {
                        existingItem.distribution = buildItem.getDistribution();
                    } else if (buildItem.getDistribution() != null
                            && !existingItem.distribution.equals(buildItem.getDistribution())) {
                        // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the distribution
                        throw new BuildException(
                                "Multiple extensions request different distributions of Elasticsearch for Dev Services.",
                                Collections.emptyList());
                    }
                    existingItem.hostsConfigProperties.add(buildItem.getHostsConfigProperty());
                }
            }

            return result.values();
        }
    }
}
