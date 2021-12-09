package io.quarkus.mtls.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.mtls.MutualTLSProvider;

/**
 * Utility for locating mutual TLS providers.
 */
public class MutualTLSProviderFinder {

    /**
     * Find a mutual TLS provider, either by type or the default implementation.
     *
     * @apiNote The type is equivalent to a CDI bean name (e.g. using the @Named annotation).
     * @param type Type of provider to find or null for default implementation.
     * @return Mutual TLS Provider for the given type.
     * @throws RuntimeException if no provider of the given type exists.
     */
    public static MutualTLSProvider find(String type) {
        ArcContainer container = Arc.container();
        MutualTLSProvider mutualTLSProvider = type != null
                ? (MutualTLSProvider) container.instance(type).get()
                : container.instance(MutualTLSProvider.class).get();

        if (mutualTLSProvider == null) {
            throw new RuntimeException("unable to find mutual TLS provider of type " + (type == null ? "default" : type));
        }

        return mutualTLSProvider;
    }

}
