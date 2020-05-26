package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

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
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * <p>
     * For Vault it is: vault-credentials-provider. Not necessary if there is only one credentials provider available.
     */
    @ConfigItem
    public Optional<String> credentialsProviderName = Optional.empty();
}
