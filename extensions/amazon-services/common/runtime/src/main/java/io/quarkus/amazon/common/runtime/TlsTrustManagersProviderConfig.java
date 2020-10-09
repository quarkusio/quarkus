package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TlsTrustManagersProviderConfig {

    // @formatter:off
    /**
     * TLS trust managers provider type.
     *
     * Available providers:
     *
     * * `trust-all` - Use this provider to disable the validation of servers certificates and therefor turst all server certificates.
     * * `system-property` - Provider checks the standard `javax.net.ssl.keyStore`, `javax.net.ssl.keyStorePassword`, and
     *                       `javax.net.ssl.keyStoreType` properties defined by the
     *                        https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html[JSSE].
     * * `file-store` - Provider that loads a the key store from a file.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem(defaultValue = "system-property")
    public TlsTrustManagersProviderType type;

    /**
     * Configuration of the file store provider.
     * <p>
     * Used only if {@code FILE_STORE} type is chosen.
     */
    @ConfigItem
    public FileStoreTlsManagersProviderConfig fileStore;

}
