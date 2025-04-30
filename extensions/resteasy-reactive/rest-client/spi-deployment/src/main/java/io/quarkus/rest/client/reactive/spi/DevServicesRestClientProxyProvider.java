package io.quarkus.rest.client.reactive.spi;

import java.io.Closeable;

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
     * Called by Quarkus for each of the REST Clients that need to be proxied
     */
    CreateResult create(RestClientHttpProxyBuildItem buildItem);

    record CreateResult(String host, Integer port, Closeable closeable) {

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
