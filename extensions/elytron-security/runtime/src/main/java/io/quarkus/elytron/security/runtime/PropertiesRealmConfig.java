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
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * If the properties store is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The location of the users property resource
     */
    @ConfigItem(defaultValue = "users.properties")
    public String users;

    /**
     * The location of the roles property file
     */
    @ConfigItem(defaultValue = "roles.properties")
    public String roles;

    public String help() {
        return "{enabled,users,roles,authMechanism,realmName}";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String toString() {
        return "PropertiesRealmConfig{" +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                ", users='" + users + '\'' +
                ", roles='" + roles + '\'' +
                '}';
    }
}
