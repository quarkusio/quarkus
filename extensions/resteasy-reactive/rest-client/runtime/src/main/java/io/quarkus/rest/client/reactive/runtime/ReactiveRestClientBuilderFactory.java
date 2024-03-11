package io.quarkus.rest.client.reactive.runtime;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.restclient.config.RestClientBuilderFactory;
import io.quarkus.restclient.config.RestClientsConfig;

public class ReactiveRestClientBuilderFactory implements RestClientBuilderFactory {

    public RestClientBuilder newBuilder(Class<?> proxyType, RestClientsConfig restClientsConfigRoot) {
        RegisterRestClient annotation = proxyType.getAnnotation(RegisterRestClient.class);
        String configKey = null;
        String baseUri = null;
        if (annotation != null) {
            configKey = annotation.configKey();
            baseUri = annotation.baseUri();
        }

        RestClientBuilderImpl restClientBuilder = new RestClientBuilderImpl();
        QuarkusRestClientBuilderImpl quarkusRestClientBuilder = new QuarkusRestClientBuilderImpl(restClientBuilder);
        RestClientCDIDelegateBuilder<?> restClientBase = new RestClientCDIDelegateBuilder<>(proxyType, baseUri, configKey,
                restClientsConfigRoot);
        restClientBase.configureBuilder(quarkusRestClientBuilder);

        return restClientBuilder;
    }

}
