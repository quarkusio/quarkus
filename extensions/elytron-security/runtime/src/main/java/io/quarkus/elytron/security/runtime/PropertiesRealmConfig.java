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
     * Name of authentication mechanism to use
     */
    @ConfigItem(defaultValue = "BASIC")
    public String authMechanism;

    /**
     * Name to assign the security realm
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * Determine whether security via the file realm is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The location of the users property resource.
     * This is a classpath resource name of properties file containing user to password mappings; see <<Users.properties>>
     */
    @ConfigItem(defaultValue = "users.properties")
    public String users;

    /**
     * The location of the roles property file.
     * This is a classpath resource name of properties file containing user to role mappings; see <<Roles.properties>>
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

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * Used to access what should be a parent class, but parsing of the MP config properties is not working
     * from parent to child
     *
     * @return AuthConfig information
     */
    public AuthConfig getAuthConfig() {
        return new AuthConfig(authMechanism, realmName, getClass());
    }

    @Override
    public String toString() {
        return "PropertiesRealmConfig{" +
                "authMechanism='" + authMechanism + '\'' +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                ", users='" + users + '\'' +
                ", roles='" + roles + '\'' +
                '}';
    }
}
