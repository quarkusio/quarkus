package io.quarkus.rest.client.reactive.deployment.devservices;

import java.io.IOException;
import java.net.URI;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.rest.client.reactive.spi.DevServicesRestClientProxyProvider;
import io.quarkus.rest.client.reactive.spi.DevServicesRestClientProxyProvider.CreateResult;
import io.quarkus.rest.client.reactive.spi.RestClientHttpProxyBuildItem;

/**
 * The local proxy of a REST Client, as a Dev Service.
 */
final class RestClientProxyStartable implements Startable {
    static final String ORIGIN = "origin";

    private final DevServicesRestClientProxyProvider provider;
    private final RestClientHttpProxyBuildItem proxy;
    private volatile CreateResult result;

    RestClientProxyStartable(DevServicesRestClientProxyProvider provider, RestClientHttpProxyBuildItem proxy) {
        this.provider = provider;
        this.proxy = proxy;
    }

    @Override
    public void start() {
        result = provider.create(proxy);
    }

    /**
     * Points the proxy to the origin, the resolved base URI of the REST Client, which the application publishes on every
     * start.
     */
    void originAvailable(URI origin) {
        result.origin().accept(origin);
    }

    @Override
    public String getConnectionInfo() {
        return String.format("http://%s:%d", result.host(), result.port());
    }

    @Override
    public String getContainerId() {
        return null;
    }

    @Override
    public void close() throws IOException {
        if (result != null && result.closeable() != null) {
            result.closeable().close();
        }
    }
}
