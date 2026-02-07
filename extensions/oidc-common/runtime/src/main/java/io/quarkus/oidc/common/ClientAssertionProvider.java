package io.quarkus.oidc.common;

import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;

/**
 * Client assertion provider. Depending on {@link OidcClientCommonConfig.Credentials.Jwt#source()}, OIDC client can use
 * provided client assertion to authenticate with OIDC endpoints. The implementation can be registered as a CDI bean.
 */
public interface ClientAssertionProvider {

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
    default void close() {
        // do nothing by default
    }

    /**
     * This method allows to restrict OIDC tenants or clients for which the client assertion is provided.
     * By default, the provider is always applied. If multiple providers applies for the {@code name},
     * the first found provider is applied.
     *
     * @param id a unique OIDC client identifier or OIDC tenant id
     * @param source JWT source
     * @return true if Quarkus should use this provider to get the client assertion
     */
    default boolean appliesTo(String id, OidcClientCommonConfig.Credentials.Jwt.Source source) {
        return true;
    }
}
