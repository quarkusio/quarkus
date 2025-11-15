package io.quarkus.spring.cloud.config.client.runtime;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.logging.Logger;

import io.quarkus.spring.cloud.config.client.runtime.util.UrlUtility;

public class DirectConfigServerBaseUrlProvider implements ConfigServerBaseUrlProvider {

    private static final Logger log = Logger.getLogger(DirectConfigServerBaseUrlProvider.class);
    private final SpringCloudConfigClientConfig config;

    public DirectConfigServerBaseUrlProvider(SpringCloudConfigClientConfig config) {
        this.config = config;
    }

    @Override
    public URI get() {
        log.info("Getting config server URL with Direct ConfigServer BaseUrl");
        String url = config.url();
        validate(url);
        try {
            return UrlUtility.toURI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + config.url()
                    + "' of property 'quarkus.spring-cloud-config.url' is invalid", e);
        }
    }

    private void validate(String url) {
        if (null == url || url.isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'quarkus.spring-cloud-config.url' property cannot be empty");
        }
    }
}
