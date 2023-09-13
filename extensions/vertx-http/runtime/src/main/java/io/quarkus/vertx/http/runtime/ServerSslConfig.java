package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.DefaultConverter;

/**
 * Shared configuration for setting up server-side SSL.
 */
@ConfigGroup
public class ServerSslConfig {
    /**
     * The server certificate configuration.
     */
    public CertificateConfig certificate;

    /**
     * The cipher suites to use. If none is given, a reasonable default is selected.
     */
    @ConfigItem
    public Optional<List<String>> cipherSuites;

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
    @DefaultConverter
    @ConfigItem(defaultValue = "TLSv1.3,TLSv1.2")
    public Set<String> protocols;

    /**
     * Enables Server Name Indication (SNI), an TLS extension allowing the server to use multiple certificates.
     * The client indicate the server name during the TLS handshake, allowing the server to select the right certificate.
     */
    @ConfigItem(defaultValue = "false")
    public boolean sni;

}
