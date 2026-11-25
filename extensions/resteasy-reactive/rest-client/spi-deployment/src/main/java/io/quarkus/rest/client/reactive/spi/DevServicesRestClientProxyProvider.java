package io.quarkus.rest.client.reactive.spi;

import java.io.Closeable;
import java.net.URI;
import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Extensions that integrate with the REST Client can use this interface in order to provide their own proxy
 * when users have {@code quarkus.rest-client."full-class-name".enable-local-proxy"} enabled, can implement
 * this interface and register it by producing {@link BuildItem}.
 */
public interface DevServicesRestClientProxyProvider {

    /**
     * Used by Quarkus to determine which provider to use when multiple providers exist.
     * User control this if necessary by setting {@code quarkus.rest-client."full-class-name".local-proxy-provider}
     */
    String name();

    /**
     * Called once by Quarkus to allow the provider to initialize
     */
    Closeable setup();

    /**
     * Called by Quarkus for each of the REST Clients that need to be proxied.
     * <p>
     * The proxy starts before the application, so it starts without an origin. The base URI of the REST Client comes
     * from the runtime configuration of the application, and may reference an address of the application, such as
     * {@code quarkus://vertx-http}, that is only bound once the application starts. The application resolves it on
     * every start, and hands it to the {@link CreateResult#origin()} consumer.
     */
    CreateResult create(RestClientHttpProxyBuildItem buildItem);

    record CreateResult(String host, Integer port, Consumer<URI> origin, Closeable closeable) {
    }

    /**
     * Build item used to register the provider with Quarkus
     */
    final class BuildItem extends MultiBuildItem {

        private final DevServicesRestClientProxyProvider provider;

        public BuildItem(DevServicesRestClientProxyProvider provider) {
            this.provider = provider;
        }

        public DevServicesRestClientProxyProvider getProvider() {
            return provider;
        }
    }
}
