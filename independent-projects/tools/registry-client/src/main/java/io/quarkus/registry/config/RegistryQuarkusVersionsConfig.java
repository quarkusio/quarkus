package io.quarkus.registry.config;

/**
 * A registry may be configured to accept requests only for the Quarkus versions
 * it recognizes. This may avoid unnecessary remote registry requests from the client.
 */
public interface RegistryQuarkusVersionsConfig {

    /**
     * An expression that will be evaluated on the client side before sending
     * a request to the registry that will indicate whether the registry recognizes
     * a given Quarkus version or not.
     *
     * @return Quarkus version filtering expression or null
     */
    String getRecognizedVersionsExpression();

    /**
     * If the Quarkus version expression is provided, this method may also enforce that
     * Quarkus versions matching the provided expressions are expected to be provided
     * by this registry exclusively. This may further reduce the amount of the remote requests
     * a client will be sending in case multiple registries have been configured.
     *
     * @return whether the registry is an exclusive provider of the Quarkus versions matching
     *         the expression configured in {@link getRecognizedVersionsExpression}
     */
    boolean isExclusiveProvider();
}
