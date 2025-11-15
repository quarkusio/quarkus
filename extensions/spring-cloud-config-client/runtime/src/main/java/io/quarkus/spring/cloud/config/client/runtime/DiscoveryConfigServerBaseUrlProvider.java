package io.quarkus.spring.cloud.config.client.runtime;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.logging.Logger;

import io.quarkus.spring.cloud.config.client.runtime.eureka.DiscoveryService;
import io.quarkus.spring.cloud.config.client.runtime.util.UrlUtility;

public class DiscoveryConfigServerBaseUrlProvider implements ConfigServerBaseUrlProvider {

    private static final Logger log = Logger.getLogger(DiscoveryConfigServerBaseUrlProvider.class);
    private final DiscoveryService discoveryService;
    private final SpringCloudConfigClientConfig config;

    public DiscoveryConfigServerBaseUrlProvider(DiscoveryService discoveryService, SpringCloudConfigClientConfig config) {
        this.discoveryService = discoveryService;
        this.config = config;
    }

    @Override
    public URI get() {
        log.info("Getting config server URL with Discovery ConfigServer");
        try {
            return UrlUtility.toURI(discoveryService.discover(config));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
