package io.quarkus.tls.runtime.config;

import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.tls.runtime.TrustStoreProvider;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

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
     * Configures generic trust stores from other providers
     */
    @ConfigDocMapKey("trust-store-type")
    @WithParentName
    Map<String, GenericTrustStoreConfig> generic();

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
        var enabledTrustStoreTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        if (provider.isAvailable()) {
            enabledTrustStoreTypes.add("a provider");
        }
        pem().ifPresent(c -> enabledTrustStoreTypes.add("PEM"));
        p12().ifPresent(c -> enabledTrustStoreTypes.add("P12"));
        jks().ifPresent(c -> enabledTrustStoreTypes.add("JKS"));
        enabledTrustStoreTypes.addAll(generic().keySet());

        if (enabledTrustStoreTypes.size() > 1) {
            throw new IllegalStateException("Invalid truststore '" + name + "' - The truststore cannot be configured with "
                    + String.join(" and ", enabledTrustStoreTypes) + " at the same time");
        }
    }

}
