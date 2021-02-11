package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TlsKeyManagersProviderConfig {

    // @formatter:off
    /**
     * TLS key managers provider type.
     *
     * Available providers:
     *
     * * `none` - Use this provider if you don't want the client to present any certificates to the remote TLS host.
     * * `system-property` - Provider checks the standard `javax.net.ssl.keyStore`, `javax.net.ssl.keyStorePassword`, and
     *                       `javax.net.ssl.keyStoreType` properties defined by the
     *                        https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html[JSSE].
     * * `file-store` - Provider that loads a the key store from a file.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem(defaultValue = "system-property")
    public TlsKeyManagersProviderType type;

    /**
     * Configuration of the file store provider.
     * <p>
     * Used only if {@code FILE_STORE} type is chosen.
     */
    @ConfigItem
    public FileStoreTlsManagersProviderConfig fileStore;

}
