package io.quarkus.oidc.common.runtime;

import io.smallrye.mutiny.Uni;

/**
 * Client assertion provider.
 */
public sealed interface ClientAssertionProvider permits KubernetesServiceClientAssertionProvider {

    /**
     * Gets current client assertion.
     *
     * @return {@link Uni} which resolves to a client assertion
     */
    Uni<String> getClientAssertion();

    /**
     * Gets the client assertion type.
     *
     * @return client assertion type
     */
    String getClientAssertionType();

    /**
     * Closes this provider. Use this method to cancel the periodic scheduled task.
     */
    void close();

}
