package io.quarkus.rest.client.reactive.runtime;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.rest.client.reactive.RestClientBuilderImpl;

public class BuilderResolver extends RestClientBuilderResolver {
    @Override
    public RestClientBuilder newBuilder() {
        return new RestClientBuilderImpl();
    }
}
