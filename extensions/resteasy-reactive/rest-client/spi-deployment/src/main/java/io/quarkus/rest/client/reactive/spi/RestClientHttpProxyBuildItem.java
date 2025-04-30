package io.quarkus.rest.client.reactive.spi;

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents the data necessary for creating a Http proxy for a REST Client
 */
public final class RestClientHttpProxyBuildItem extends MultiBuildItem {

    private final String className;
    private final String baseUri;
    private final Optional<String> provider;

    // this is only used to make bookkeeping easier
    private volatile Closeable closeable;

    public RestClientHttpProxyBuildItem(String className, String baseUri, Optional<String> provider) {
        this.className = Objects.requireNonNull(className);
        this.baseUri = Objects.requireNonNull(baseUri);
        this.provider = provider;
    }

    public String getClassName() {
        return className;
    }

    public String getBaseUri() {
        return baseUri;
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
        return Objects.equals(className, that.className) && Objects.equals(baseUri, that.baseUri)
                && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, baseUri, provider);
    }

    /**
     * Called by Quarkus in order to associate a {@link Closeable} with a started proxy
     */
    public void attachClosable(Closeable closeable) {
        this.closeable = closeable;
    }

    /**
     * Called by Quarkus when it's time to stop the proxy
     */
    public Closeable getCloseable() {
        return closeable;
    }
}
