package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.smallrye.config.WithDefault;

/**
 * Shared configuration for setting up server-side SSL.
 */
public interface ServerSslConfig {
    /**
     * The server certificate configuration.
     */
    CertificateConfig certificate();

    /**
     * The cipher suites to use. If none is given, a reasonable default is selected.
     */
    Optional<List<String>> cipherSuites();

    /**
     * Sets the ordered list of enabled SSL/TLS protocols.
     * <p>
     * If not set, it defaults to {@code "TLSv1.3, TLSv1.2"}.
     * The following list of protocols are supported: {@code TLSv1, TLSv1.1, TLSv1.2, TLSv1.3}.
     * To only enable {@code TLSv1.3}, set the value to {@code to "TLSv1.3"}.
     * <p>
     * Note that setting an empty list, and enabling SSL/TLS is invalid.
     * You must at least have one protocol.
     */
    @WithDefault("TLSv1.3,TLSv1.2")
    Set<String> protocols();

    /**
     * Enables Server Name Indication (SNI), an TLS extension allowing the server to use multiple certificates.
     * The client indicate the server name during the TLS handshake, allowing the server to select the right certificate.
     */
    @WithDefault("false")
    boolean sni();

}
