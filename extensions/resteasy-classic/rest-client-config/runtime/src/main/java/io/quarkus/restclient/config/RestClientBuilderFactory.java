package io.quarkus.restclient.config;

import java.util.ServiceLoader;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

/**
 * Factory which creates MicroProfile RestClientBuilder instance configured according to current Quarkus application
 * configuration.
 *
 * The builder instance can be further tweaked, if needed, before building the rest client proxy.
 */
public interface RestClientBuilderFactory {

    default RestClientBuilder newBuilder(Class<?> proxyType) {
        return newBuilder(proxyType, RestClientsConfig.getInstance());
    }

    RestClientBuilder newBuilder(Class<?> proxyType, RestClientsConfig restClientsConfigRoot);

    static RestClientBuilderFactory getInstance() {
        ServiceLoader<RestClientBuilderFactory> sl = ServiceLoader.load(RestClientBuilderFactory.class);
        RestClientBuilderFactory instance = null;
        for (RestClientBuilderFactory spi : sl) {
            if (instance != null) {
                throw new IllegalStateException("Multiple RestClientBuilderFactory implementations found: "
                        + spi.getClass().getName() + " and "
                        + instance.getClass().getName());
            }
            instance = spi;
        }
        return instance;
    }
}
