package io.quarkus.elytron.security.properties.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * A configuration object for a properties resource based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
 * It consists of a users.properties that has the format:
 * user1=password1
 * user2=password2
 *
 * and a roles.properties that has the format:
 * user1=role1,role2,...,roleN1
 * user2=role21,role2,...,roleN2
 */
@ConfigGroup
public interface PropertiesRealmConfig {

    /**
     * The realm name. This is used when generating a hashed password
     */
    @WithDefault("Quarkus")
    String realmName();

    /**
     * Determine whether security via the file realm is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * If the properties are stored in plain text. If this is false (the default) then it is expected
     * that the passwords are of the form HEX( MD5( username ":" realm ":" password ) )
     */
    @WithDefault("false")
    boolean plainText();

    /**
     * Classpath resource name of properties file containing user to password mappings. See
     * <a href="#users-properties">Users.properties</a>.
     */
    @WithDefault("users.properties")
    String users();

    /**
     * Classpath resource name of properties file containing user to role mappings. See
     * <a href="#roles-properties">Roles.properties</a>.
     */
    @WithDefault("roles.properties")
    String roles();

    static String help() {
        return "{enabled,users,roles,realm-name,plain-text}";
    }

    String toString();
}
