package io.quarkus.restclient.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RestClientConfigRootProvider {

    @Inject
    RestClientConfigRoot configRoot;

    public RestClientConfigRoot getConfigRoot() {
        return configRoot;
    }

    public RestClientConfig getConfig(String configKey) {
        return configRoot.configs.get(configKey);
    }
}
