package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.tls.OtherTrustStoreConfiguration;

/**
 * Configuration for a trust store with an arbitrary type, used for trust store formats not directly supported
 * by the TLS registry (such as BCFKS or PKCS#11).
 * <p>
 * The loading strategy depends on whether a {@code TrustStoreFactory} CDI bean annotated with
 * {@code @Identifier("<type>")} exists:
 * <ul>
 * <li><strong>Factory:</strong> If a {@code TrustStoreFactory} bean matching the {@link #type()} is found,
 * it is always used. The factory receives this entire configuration, including {@link #params()}. Use this for stores
 * that require custom initialization, such as PKCS#11 hardware tokens or credential frameworks.</li>
 * <li><strong>Standard fallback (file-backed):</strong> If no factory is found, the store is loaded using
 * {@code java.security.KeyStore.getInstance(type, provider)} followed by {@code keyStore.load(inputStream, password)}.
 * In this case, {@link #path()} is required. This works for any file-backed trust store type supported by the JVM
 * or a registered security provider (e.g., BCFKS).</li>
 * </ul>
 */
@ConfigGroup
public interface OtherTrustStoreConfig extends OtherTrustStoreConfiguration {

    /**
     * The trust store type, passed to {@code java.security.KeyStore.getInstance(type)}.
     * <p>
     * This value also serves as the lookup key for a {@code TrustStoreFactory} CDI bean via {@code @Identifier}.
     * If a matching factory is found, it takes priority over the standard file-based loading.
     * <p>
     * Examples: {@code BCFKS}, {@code PKCS11}.
     */
    String type();

    /**
     * The Java security provider name, passed to {@code java.security.KeyStore.getInstance(type, provider)}.
     * <p>
     * Used by the standard fallback loading. If not set, the default provider for the given type is used.
     * A factory may also read this value but is not required to.
     */
    Optional<String> provider();

    /**
     * Path to the trust store file.
     * <p>
     * Required when using the standard fallback (no factory). When a factory handles the loading,
     * this value is available to the factory via this config but is not used by the TLS registry itself.
     */
    Optional<Path> path();

    /**
     * Password of the trust store.
     * <p>
     * Used by the standard fallback to load the store. A factory may also read this value.
     * When not set, the password can be retrieved from the credential provider configured on the parent trust store.
     */
    Optional<String> password();

    /**
     * Alias of the certificate in the trust store.
     * <p>
     * Used by the standard fallback to validate that a specific certificate exists.
     * A factory may also read this value but is not required to.
     */
    Optional<String> alias();

    /**
     * Arbitrary parameters available to a {@code TrustStoreFactory} CDI bean via {@code config.params()}.
     * <p>
     * Ignored by the standard fallback. Set via {@code quarkus.tls.trust-store.other.params.<key>=<value>}.
     */
    Map<String, String> params();
}
