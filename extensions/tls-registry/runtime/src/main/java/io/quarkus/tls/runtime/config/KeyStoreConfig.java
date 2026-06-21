package io.quarkus.tls.runtime.config;

import java.util.Optional;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.tls.KeyStoreProvider;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface KeyStoreConfig {

    /**
     * Configures the PEM key/certificate pair.
     */
    Optional<PemKeyCertConfig> pem();

    /**
     * Configure the PKCS12 key store.
     */
    Optional<P12KeyStoreConfig> p12();

    /**
     * Configure the JKS key store.
     */
    Optional<JKSKeyStoreConfig> jks();

    /**
     * Configure a key store with an arbitrary type.
     * Use this for key store types not directly supported (e.g., BCFKS or PKCS11).
     * <p>
     * Note that when using this option, the key store can either be created using a path/password fallback or use a
     * specific {@link io.quarkus.tls.KeyStoreFactory} implementation.
     * In the first case, the key store will be created using the configured path and password.
     */
    Optional<OtherKeyStoreConfig> other();

    /**
     * Enables Server Name Indication (SNI).
     * <p>
     * Server Name Indication (SNI) is a TLS extension that allows a client to specify the hostname it is attempting to
     * connect to during the TLS handshake. This enables a server to present different SSL certificates for multiple
     * domains on a single IP address, facilitating secure communication for virtual hosting scenarios.
     * <p>
     * With this setting enabled, the client indicate the server name during the TLS handshake, allowing the server to
     * select the right certificate.
     * <p>
     * When configuring the keystore with PEM files, multiple CRT/Key must be given.
     * When configuring the keystore with a JKS or a P12 file, it selects one alias based on the SNI hostname.
     * In this case, all the keystore password and alias password must be the same (configured with the {@code password}
     * and {@code alias-password} properties. Do not set the {@code alias} property.
     */
    @WithDefault("false")
    boolean sni();

    /**
     * The credential provider configuration for the keys store.
     * A credential provider offers a way to retrieve the key store password and alias password.
     * Note that the credential provider is only used if the password / alias password are not set in the configuration.
     */
    KeyStoreCredentialProviderConfig credentialsProvider();

    default void validate(InstanceHandle<KeyStoreProvider> provider, String name) {
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
                    "Invalid keystore '" + name
                            + "' - The keystore cannot be configured with a provider and PEM, PKCS12, JKS, or other at the same time");
        }

        if (count > 1) {
            throw new IllegalStateException(
                    "Invalid keystore '" + name
                            + "' - Only one keystore type can be configured at a time (PEM, PKCS12, JKS, or other)");
        }
    }

}
