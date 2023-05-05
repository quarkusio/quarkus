package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

@ConfigGroup
public interface DataSourceRuntimeConfig {

    /**
     * The datasource username
     */
    Optional<String> username();

    /**
     * The datasource password
     */
    Optional<String> password();

    /**
     * The credentials provider name
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProvider();

    /**
     * The credentials provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * <p>
     * For Vault it is: vault-credentials-provider. Not necessary if there is only one credentials provider available.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProviderName();
}
