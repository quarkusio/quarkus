package io.quarkus.tls.runtime.config;

import java.util.Optional;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.tls.TrustStoreProvider;
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
     * Configure a trust store with an arbitrary type.
     * Use this for trust store types not directly supported (e.g., BCFKS).
     * <p>
     * Note that when using this option, the trust store can either be created using a path/password fallback or use a
     * specific {@link io.quarkus.tls.TrustStoreFactory} implementation.
     * In the first case, the trust store will be created using the configured path and password.
     */
    Optional<OtherTrustStoreConfig> other();

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
        int count = 0;
        if (pem().isPresent())
            count++;
        if (p12().isPresent())
            count++;
        if (jks().isPresent())
            count++;
        if (other().isPresent())
            count++;

        if (provider.isAvailable() && count > 0) {
            throw new IllegalStateException(
                    "Invalid truststore '" + name
                            + "' - The truststore cannot be configured with a provider and PEM, PKCS12, JKS, or other at the same time");
        }

        if (count > 1) {
            throw new IllegalStateException(
                    "Invalid truststore '" + name
                            + "' - Only one truststore type can be configured at a time (PEM, PKCS12, JKS, or other)");
        }
    }

}
