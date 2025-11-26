package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.testcontainers.utility.DockerImageName;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;

final class DevservicesElasticsearchProcessorUtils {

    /**
     * Label to add to shared Dev Service for Elasticsearch running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-elasticsearch";
    static final String NEW_DEV_SERVICE_LABEL = "io.quarkus.devservice.elasticsearch";
    static final String DEV_SERVICE_ELASTICSEARCH = "elasticsearch";
    static final String DEV_SERVICE_OPENSEARCH = "opensearch";
    static final int ELASTICSEARCH_PORT = 9200;
    static final Distribution DEFAULT_DISTRIBUTION = Distribution.ELASTIC;

    private DevservicesElasticsearchProcessorUtils() {
    }

    static String getElasticsearchHosts(DevservicesElasticsearchBuildItemsConfiguration buildItemsConfiguration,
            RunningDevService devService) {
        String hostsConfigProperty = buildItemsConfiguration.hostsConfigProperties.stream().findAny().get();
        return devService.getConfig().get(hostsConfigProperty);
    }

    static Distribution resolveDistribution(ElasticsearchDevServicesBuildTimeConfig config,
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

    static Map<String, String> buildPropertiesMap(DevservicesElasticsearchBuildItemsConfiguration buildItemConfig,
            String httpHosts) {
        Map<String, String> propertiesToSet = new HashMap<>();
        for (String property : buildItemConfig.hostsConfigProperties) {
            propertiesToSet.put(property, httpHosts);
        }
        return propertiesToSet;
    }
}
