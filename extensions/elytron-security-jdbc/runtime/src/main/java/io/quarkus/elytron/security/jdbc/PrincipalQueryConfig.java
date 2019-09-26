package io.quarkus.elytron.security.jdbc;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.jdbc.QueryBuilder}
 */
@ConfigGroup
public class PrincipalQueryConfig {

    /**
     * The sql query to find the password
     */
    @ConfigItem
    public String sql;

    /**
     * The data source to use
     */
    @ConfigItem
    public Optional<String> datasource;

    /**
     * The definitions of the mapping between the database columns and the identity's attributes
     */
    @ConfigItem
    public Map<String, AttributeMappingConfig> attributeMappings;

    /**
     * The "clear-password-mapper" configuration
     */
    @ConfigItem(name = "clear-password-mapper")
    public ClearPasswordMapperConfig clearPasswordMapperConfig;

    /**
     * The "bcrypt-password-mapper" configuration
     */
    @ConfigItem(name = "bcrypt-password-mapper")
    public BcryptPasswordKeyMapperConfig bcryptPasswordKeyMapperConfig;

    @Override
    public String toString() {
        return "PrincipalQueryConfig{" +
                "sql='" + sql + '\'' +
                ", datasource='" + datasource + '\'' +
                ", attributeMappings=" + attributeMappings +
                ", clearPasswordMapperConfig=" + clearPasswordMapperConfig +
                ", bcryptPasswordKeyMapperConfig=" + bcryptPasswordKeyMapperConfig +
                '}';
    }

}
