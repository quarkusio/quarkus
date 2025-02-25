package io.quarkus.tls.runtime.config;

import java.util.Optional;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.tls.runtime.TrustStoreProvider;
import io.smallrye.config.WithDefault;

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
     * Enforce certificate expiration.
     * When enabled, the certificate expiration date is verified and the certificate (or any certificate in the chain)
     * is rejected if it is expired.
     */
    @WithDefault("WARN")
    CertificateExpiryPolicy certificateExpirationPolicy();

    /**
     * The policy to apply when a certificate is expired.
     */
    enum CertificateExpiryPolicy {
        /**
         * Ignore the expiration date.
         */
        IGNORE,
        /**
         * Log a warning when the certificate is expired.
         */
        WARN,
        /**
         * Reject the certificate if it is expired.
         */
        REJECT
    }

    /**
     * The credential provider configuration for the trust store.
     * A credential provider offers a way to retrieve the trust store password.
     * Note that the credential provider is only used if the password is not set in the configuration.
     */
    TrustStoreCredentialProviderConfig credentialsProvider();

    default void validate(InstanceHandle<TrustStoreProvider> provider, String name) {
        if (provider.isAvailable() && (pem().isPresent() || p12().isPresent() || jks().isPresent())) {
            throw new IllegalStateException(
                    "Invalid truststore '" + name
                            + "' - The truststore cannot be configured with a provider and PEM or PKCS12 or JKS at the same time");
        }

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
