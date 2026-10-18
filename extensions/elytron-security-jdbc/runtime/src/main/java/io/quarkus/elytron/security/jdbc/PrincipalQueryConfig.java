package io.quarkus.elytron.security.jdbc;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithName;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.jdbc.QueryBuilder}
 */
@ConfigGroup
public interface PrincipalQueryConfig {

    /**
     * The sql query to find the password
     */
    Optional<String> sql();

    /**
     * The data source to use
     */
    Optional<String> datasource();

    /**
     * The definitions of the mapping between the database columns and the identity's attributes
     */
    Map<String, AttributeMappingConfig> attributeMappings();

    /**
     * The "clear-password-mapper" configuration
     */
    @WithName("clear-password-mapper")
    ClearPasswordMapperConfig clearPasswordMapperConfig();

    /**
     * The "bcrypt-password-mapper" configuration
     */
    @WithName("bcrypt-password-mapper")
    BcryptPasswordKeyMapperConfig bcryptPasswordKeyMapperConfig();

    String toString();
}
