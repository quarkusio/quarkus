package io.quarkus.runtime.configuration.ssl;

import java.util.List;

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
    public List<String> cipherSuites;

    /**
     * The list of protocols to explicitly enable.
     */
    @DefaultConverter
    @ConfigItem(defaultValue = "TLSv1.3,TLSv1.2")
    public List<String> protocols;

}
