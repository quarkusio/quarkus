package io.quarkus.spring.cloud.config.client.runtime.eureka;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientConfig;
import io.vertx.core.json.JsonObject;

public class DiscoveryService {

    private static final Logger log = Logger.getLogger(DiscoveryService.class);
    private static final String DEFAULT_ZONE = "defaultZone";

    private final EurekaClient eurekaClient;

    public DiscoveryService(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    public String discover(SpringCloudConfigClientConfig config) {
        SpringCloudConfigClientConfig.DiscoveryConfig discoveryConfig = config.discovery().get();
        validate(discoveryConfig);

        String serviceId = discoveryConfig.serviceId().get();
        SpringCloudConfigClientConfig.DiscoveryConfig.EurekaConfig eurekaConfig = discoveryConfig.eurekaConfig().get();
        String defaultServiceUrl = eurekaConfig.serviceUrl().get(DEFAULT_ZONE);

        Map<String, String> serviceUrlMap = eurekaConfig
                .serviceUrl()
                .entrySet()
                .stream().filter(entry -> !DEFAULT_ZONE.equals(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Attempting to discover Spring Cloud Config Server URL for service '" + serviceId
                + "' using the following URLs: " + serviceUrlMap.values());
        for (Map.Entry<String, String> entry : serviceUrlMap.entrySet()) {
            try {
                return getHomeUrl(entry.getValue(), serviceId);
            } catch (Exception e) {
                log.debug("Timed out while waiting for Spring Cloud Config Server URL for service '" + serviceId + "'", e);
            }
        }

        log.debug("Fallback Attempting to discover Spring Cloud Config Server URL for service '" + serviceId
                + "' using the default URL: " + defaultServiceUrl);
        try {
            return getHomeUrl(defaultServiceUrl, serviceId);
        } catch (Exception e) {
            log.debug("Timed out while waiting for Spring Cloud Config Server URL for service '" + serviceId + "'", e);
        }

        throw new RuntimeException("Unable to discover Spring Cloud Config Server URL for service '" + serviceId + "'");
    }

    private void validate(SpringCloudConfigClientConfig.DiscoveryConfig discoveryConfig) {
        if (discoveryConfig.eurekaConfig().isEmpty()) {
            throw new IllegalArgumentException("No Eureka configuration has been provided");
        }
        if (discoveryConfig.eurekaConfig().get().serviceUrl().isEmpty()) {
            throw new IllegalArgumentException("No service URLs have been configured for service");
        }
        if (discoveryConfig.serviceId().isEmpty()) {
            throw new IllegalArgumentException("No service ID has been configured for service");
        }
    }

    private String getHomeUrl(String defaultServiceUrl, String serviceId) {
        JsonObject instance = eurekaClient.fetchInstances(defaultServiceUrl, serviceId);
        return instance.getString("homePageUrl");
    }

}
