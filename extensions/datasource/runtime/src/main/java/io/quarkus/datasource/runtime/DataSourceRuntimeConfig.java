package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class DataSourceRuntimeConfig {

    /**
     * The datasource username
     */
    @ConfigItem
    public Optional<String> username = Optional.empty();

    /**
     * The datasource password
     */
    @ConfigItem
    public Optional<String> password = Optional.empty();

    /**
     * The credentials provider name
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProviderName = Optional.empty();
}
