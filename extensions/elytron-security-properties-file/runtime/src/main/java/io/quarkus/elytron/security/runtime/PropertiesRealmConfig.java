package io.quarkus.elytron.security.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

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
public class PropertiesRealmConfig {

    /**
     * The realm name. This is used when generating a hashed password
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * Determine whether security via the file realm is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * If the properties are stored in plain text. If this is false (the default) then it is expected
     * that the passwords are of the form HEX( MD5( username ":" realm ":" password ) )
     */
    @ConfigItem
    public boolean plainText;

    /**
     * Classpath resource name of properties file containing user to password mappings. See
     * <a href="#users-properties">Users.properties</a>.
     */
    @ConfigItem(defaultValue = "users.properties")
    public String users;

    /**
     * Classpath resource name of properties file containing user to role mappings. See
     * <a href="#roles-properties">Roles.properties</a>.
     */
    @ConfigItem(defaultValue = "roles.properties")
    public String roles;

    public String help() {
        return "{enabled,users,roles,realm-name,plain-text}";
    }

    @Override
    public String toString() {
        return "PropertiesRealmConfig{" +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                ", users='" + users + '\'' +
                ", roles='" + roles + '\'' +
                ", plainText=" + plainText +
                '}';
    }
}
