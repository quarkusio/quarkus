package io.quarkus.amazon.common.runtime;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FileStoreTlsManagersProviderConfig {

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
