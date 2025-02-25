package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface TlsBucketConfig {

    /**
     * The key store configuration.
     * Key stores are used to store private keys and their associated X.509 certificate chains.
     * For example, for {@code HTTPS}, it stores the server's private key and the server's certificate.
     * The certificate is used to prove the server's identity to the client.
     */
    Optional<KeyStoreConfig> keyStore();

    /**
     * The trust store configuration.
     * Trust stores are used to store certificates from trusted entities.
     * For example, for {@code HTTPS}, it stores the certificate authorities that are trusted by the server.
     * The server uses the trust store to verify the client's certificate when mTLS (client authentication) is enabled.
     */
    Optional<TrustStoreConfig> trustStore();

    /**
     * Sets the ordered list of enabled cipher suites.
     * If none is given, a reasonable default is selected from the built-in ciphers.
     * <p>
     * When suites are set, it takes precedence over the default suite defined by the {@code SSLEngineOptions} in use.
     */
    Optional<List<String>> cipherSuites();

    /**
     * Sets the ordered list of enabled TLS protocols.
     * <p>
     * If not set, it defaults to {@code "TLSv1.3, TLSv1.2"}.
     * The following list of protocols are supported: {@code TLSv1, TLSv1.1, TLSv1.2, TLSv1.3}.
     * To only enable {@code TLSv1.3}, set the value to {@code to "TLSv1.3"}.
     * <p>
     * Note that setting an empty list, and enabling TLS is invalid.
     * You must at least have one protocol.
     * <p>
     * Also, setting this replaces the default list of protocols.
     */
    @WithDefault("TLSv1.3,TLSv1.2")
    Set<String> protocols();

    /**
     * The timeout for the TLS handshake phase.
     * <p>
     * If not set, it defaults to 10 seconds.
     */
    @WithDefault("10S")
    Duration handshakeTimeout();

    /**
     * Enables the Application-Layer Protocol Negotiation (ALPN).
     * <p>
     * Application-Layer Protocol Negotiation is a TLS extension that allows the client and server during the TLS
     * handshake to negotiate which protocol they will use for communication. ALPN enables more efficient communication
     * by allowing the client to indicate its preferred application protocol to the server before the TLS connection is
     * established. This helps in scenarios such as HTTP/2 where multiple protocols may be available, allowing for
     * faster protocol selection.
     */
    @WithDefault("true")
    boolean alpn();

    /**
     * Sets the list of revoked certificates (paths to files).
     * <p>
     * A Certificate Revocation List (CRL) is a list of digital certificates that have been revoked by the issuing
     * Certificate Authority (CA) before their scheduled expiration date. When a certificate is compromised, no
     * longer needed, or deemed invalid for any reason, the CA adds it to the CRL to inform relying parties not to
     * trust the certificate anymore.
     * <p>
     * Two formats are allowed: DER and PKCS#7 (also known as P7B).
     * When using the DER format, you must pass DER-encoded CRLs.
     * When using the PKCS#7 format, you must pass PKCS#7 {@code SignedData} object, with the only significant field
     * being {@code crls}.
     */
    Optional<List<Path>> certificateRevocationList();

    /**
     * If set to {@code true}, the server trusts all certificates.
     * <p>
     * This is useful for testing, but should not be used in production.
     */
    @WithDefault("false")
    boolean trustAll();

    /**
     * The hostname verification algorithm to use in case the server's identity should be checked.
     * Should be {@code HTTPS} (default), {@code LDAPS} or {@code NONE}.
     * <p>
     * If set to {@code NONE}, it does not verify the hostname.
     * <p>
     * If not set, the configured extension decides the default algorithm to use.
     * For example, for HTTP, it will be "HTTPS". For TCP, it can depend on the protocol.
     * Nevertheless, it is recommended to set it to "HTTPS" or "LDAPS".
     */
    Optional<String> hostnameVerificationAlgorithm();

    /**
     * When configured, the server will reload the certificates (from the file system for example) and fires a
     * {@link CertificateUpdatedEvent} if the reload is successful
     * <p>
     * This property configures the period to reload the certificates. IF not set, the certificates won't be reloaded
     * automatically.
     * However, the application can still trigger the reload manually using the {@link io.quarkus.tls.TlsConfiguration#reload()}
     * method,
     * and then fire the {@link CertificateUpdatedEvent} manually.
     * <p>
     * The fired event is used to notify the application that the certificates have been updated, and thus proceed with the
     * actual switch of certificates.
     */
    Optional<Duration> reloadPeriod();

}
