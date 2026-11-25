package io.quarkus.rest.client.reactive.spi;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents the data necessary for creating a Http proxy for a REST Client
 */
public final class RestClientHttpProxyBuildItem extends MultiBuildItem {

    private final String className;
    private final Optional<String> provider;

    public RestClientHttpProxyBuildItem(String className, Optional<String> provider) {
        this.className = Objects.requireNonNull(className);
        this.provider = provider;
    }

    public String getClassName() {
        return className;
    }

    public Optional<String> getProvider() {
        return provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestClientHttpProxyBuildItem that = (RestClientHttpProxyBuildItem) o;
        return Objects.equals(className, that.className) && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, provider);
    }
}
