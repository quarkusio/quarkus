package io.quarkus.mailer.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class TrustStoreConfig {

    /**
     * Sets the trust store password if any.
     * Note that the password is only used for JKS and PCK#12 trust stores.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Sets the location of the trust store files.
     * If you use JKS or PCK#12, only one path is allowed.
     * If you use PEM files, you can specify multiple paths.
     * <p>
     * The relative paths are relative to the application working directly.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> paths;

    /**
     * Sets the trust store type.
     * By default, it guesses the type from the file name extension.
     * For instance, {@code truststore.pem} will be seen as a PEM file, while {@code truststore.jks} will be seen as a
     * JKS file. {@code truststore.p12} and {@code truststore.pfx} will both be seen as PCK#12 files.
     *
     * Accepted values are: {@code JKS}, {@code PEM}, {@code PCKS}.
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * @return {@code true} is the trust store is configured, {@code false otherwise}
     */
    public boolean isConfigured() {
        return paths.isPresent() && !paths.get().isEmpty();
    }
}
