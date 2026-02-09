package io.quarkus.oidc.common.runtime;

/**
 * Client assertion provider.
 */
public sealed interface ClientAssertionProvider permits KubernetesServiceClientAssertionProvider {

    /**
     * Gets current client assertion. This method should not block. If the client assertion is retrieved with blocking
     * IO operation, we recommend to keep the assertion up to date using a periodic scheduled task.
     *
     * @return client assertion
     */
    String getClientAssertion();

    /**
     * Closes this provider. Use this method to cancel the periodic scheduled task.
     */
    void close();

}
