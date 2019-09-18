package io.quarkus.dynamodb.runtime;

import java.nio.file.Path;

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
     * * `NONE` - Use this provider if you don't want the client to present any certificates to the remote TLS host.
     * * `SYSTEM_PROPERTY` - Provider checks the standard `javax.net.ssl.keyStore`, `javax.net.ssl.keyStorePassword`, and
     *                       `javax.net.ssl.keyStoreType` properties defined by the
     *                        https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html[JSSE].
     * * `FILE_STORE` - Provider that loads a the key store from a file.
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
        public Path path;

        /**
         * Key store type.
         * <p>
         * See the KeyStore section in
         * the https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore[Java Cryptography
         * Architecture Standard Algorithm Name Documentation]
         * for information about standard keystore types.
         */
        @ConfigItem
        public String type;

        /**
         * Key store password
         */
        @ConfigItem
        public String password;
    }

}
