package io.quarkus.amazon.common.runtime;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TlsManagersProviderConfig {

    // @formatter:off
    /**
     * TLS managers provider type.
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
    public TlsManagersProviderType type;

    /**
     * Configuration of the file store provider.
     * <p>
     * Used only if {@code FILE_STORE} type is chosen.
     */
    @ConfigItem
    public FileStoreTlsManagersProviderConfig fileStore;

    @ConfigGroup
    public static class FileStoreTlsManagersProviderConfig {

        /**
         * Path to the key store.
         */
        @ConfigItem
        public Optional<Path> path;

        /**
         * Key store type.
         * <p>
         * See the KeyStore section in
         * the https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore[Java Cryptography
         * Architecture Standard Algorithm Name Documentation]
         * for information about standard keystore types.
         */
        @ConfigItem
        public Optional<String> type;

        /**
         * Key store password
         */
        @ConfigItem
        public Optional<String> password;
    }

}
