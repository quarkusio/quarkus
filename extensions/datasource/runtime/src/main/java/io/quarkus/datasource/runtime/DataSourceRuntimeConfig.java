package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceRuntimeConfig {

    /**
     * Whether this datasource should be active at runtime.
     *
     * See xref:datasource.adoc#datasource-active[this section of the documentation].
     *
     * If the datasource is not active, it won't start with the application,
     * and accessing the corresponding Datasource CDI bean will fail,
     * meaning in particular that consumers of this datasource
     * (e.g. Hibernate ORM persistence units) will fail to start unless they are inactive too.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean active();

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
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProviderName();
}
