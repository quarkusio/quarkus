package io.quarkus.tls.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface TrustStoreConfig {

    /**
     * Configures the list of trusted certificates.
     */
    Optional<PemCertsConfig> pem();

    /**
     * Configure the PKCS12 trust store.
     */
    Optional<P12TrustStoreConfig> p12();

    /**
     * Configure the JKS trust store.
     */
    Optional<JKSTrustStoreConfig> jks();

    /**
     * The credential provider configuration for the trust store.
     * A credential provider offers a way to retrieve the trust store password.
     * Note that the credential provider is only used if the password is not set in the configuration.
     */
    TrustStoreCredentialProviderConfig credentialsProvider();

    default void validate(String name) {
        if (pem().isPresent() && (p12().isPresent() || jks().isPresent())) {
            throw new IllegalStateException(
                    "Invalid truststore '" + name
                            + "' - The truststore cannot be configured with PEM and PKCS12 or JKS at the same time");
        }

        if (p12().isPresent() && jks().isPresent()) {
            throw new IllegalStateException(
                    "Invalid truststore '" + name
                            + "' - The truststore cannot be configured with PKCS12 and JKS at the same time");
        }
    }

}
