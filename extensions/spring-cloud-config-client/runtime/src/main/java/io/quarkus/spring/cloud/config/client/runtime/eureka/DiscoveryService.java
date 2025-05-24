package io.quarkus.spring.cloud.config.client.runtime.eureka;

import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientConfig;
import io.vertx.core.json.JsonObject;
import org.jboss.logging.Logger;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class DiscoveryService {

    private static final Logger log = Logger.getLogger(DiscoveryService.class);
    private static final String DEFAULT_ZONE = "defaultZone";

    private final EurekaClient eurekaClient;

    public DiscoveryService(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    public String discover(SpringCloudConfigClientConfig config) {
        String serviceId = config.discovery().serviceId();

        if (config.discovery().eurekaServiceUrl().isEmpty()) {
            throw new IllegalArgumentException("No service URLs have been configured for service '" + serviceId + "'");
        }

        String defaultServiceUrl = config.discovery().eurekaServiceUrl().get(DEFAULT_ZONE);

        Map<String, String> serviceUrlMap = config.discovery()
                .eurekaServiceUrl()
                .entrySet()
                .stream().filter(entry -> !DEFAULT_ZONE.equals(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Attempting to discover Spring Cloud Config Server URL for service '" + serviceId + "' using the following URLs: " + serviceUrlMap.values());
        for (Map.Entry<String, String> entry : serviceUrlMap.entrySet()) {
            try {
                return getHomeUrl(entry.getKey(), serviceId);
            } catch (Exception e) {
                log.debug("Timed out while waiting for Spring Cloud Config Server URL for service '" + serviceId + "'", e);
            }
        }

        log.debug("Fallback Attempting to discover Spring Cloud Config Server URL for service '" + serviceId + "' using the default URL: " + defaultServiceUrl);
        try {
            return getHomeUrl(defaultServiceUrl, serviceId);
        } catch (Exception e) {
            log.debug("Timed out while waiting for Spring Cloud Config Server URL for service '" + serviceId + "'", e);
        }

        throw new RuntimeException("Unable to discover Spring Cloud Config Server URL for service '" + serviceId + "'");
    }

    private String getHomeUrl(String defaultServiceUrl, String serviceId) {
        JsonObject instance = eurekaClient.fetchInstances(defaultServiceUrl, serviceId);
        return instance.getString("homePageUrl");
    }

}
