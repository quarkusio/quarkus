package io.quarkus.mtls;

/**
 * Provides an indirection between TLS consumers such as the HTTP server and providers such as Vault.
 */
public interface MutualTLSProvider {

    /**
     * Returns the credentials for a given credentials provider
     *
     * @param mutualTLSProviderName the name of the TLS provider, which can be used to retrieve custom configuration
     * @return the mTLS configuration
     */
    MutualTLSConfig getConfig(String mutualTLSProviderName);
}
