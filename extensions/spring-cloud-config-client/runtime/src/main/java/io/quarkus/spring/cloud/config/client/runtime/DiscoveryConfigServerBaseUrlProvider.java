package io.quarkus.spring.cloud.config.client.runtime;

import io.quarkus.spring.cloud.config.client.runtime.eureka.DiscoveryService;
import io.quarkus.spring.cloud.config.client.runtime.util.UrlUtility;

import java.net.URI;
import java.net.URISyntaxException;

public class DiscoveryConfigServerBaseUrlProvider implements ConfigServerBaseUrlProvider {

    private final DiscoveryService discoveryService;
    private final SpringCloudConfigClientConfig config;

    public DiscoveryConfigServerBaseUrlProvider(DiscoveryService discoveryService, SpringCloudConfigClientConfig config) {
        this.discoveryService = discoveryService;
        this.config = config;
    }

    @Override
    public URI get() {
        try {
            return UrlUtility.toURI(discoveryService.discover(config));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
