package io.quarkus.elasticsearch.restclient.common.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.testcontainers.utility.DockerImageName;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;

final class DevservicesElasticsearchProcessorUtils {

    static final String DEV_SERVICE_ELASTICSEARCH = "elasticsearch";
    static final String DEV_SERVICE_OPENSEARCH = "opensearch";
    static final Distribution DEFAULT_DISTRIBUTION = Distribution.ELASTIC;

    private DevservicesElasticsearchProcessorUtils() {
    }

    static String getElasticsearchHosts(DevservicesElasticsearchBuildItemsConfiguration buildItemsConfiguration,
            RunningDevService devService) {
        String hostsConfigProperty = buildItemsConfiguration.hostsConfigProperties.stream().findAny().get();
        return devService.getConfig().get(hostsConfigProperty);
    }

    static Properties loadProperties(String devserviceName) {
        var fileName = devserviceName + "-devservice.properties";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalArgumentException(fileName + " not found on classpath");
            }
            var properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
