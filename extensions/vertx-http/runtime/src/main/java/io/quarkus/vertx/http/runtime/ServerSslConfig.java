package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

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
     * The list of protocols to explicitly enable.
     */
    @DefaultConverter
    @ConfigItem(defaultValue = "TLSv1.3,TLSv1.2")
    public List<String> protocols;

    /**
     * Enables Server Name Indication (SNI), an TLS extension allowing the server to use multiple certificates.
     * The client indicate the server name during the TLS handshake, allowing the server to select the right certificate.
     */
    @ConfigItem(defaultValue = "false")
    public boolean sni;

}
