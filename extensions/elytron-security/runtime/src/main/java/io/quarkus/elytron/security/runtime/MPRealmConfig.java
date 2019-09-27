package io.quarkus.elytron.security.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
 * }
 */
@ConfigGroup
public class MPRealmConfig {

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * If the embedded store is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /** The realm users user1=password\nuser2=password2... mapping */
    @ConfigItem
    public Map<String, String> users;

    /** The realm roles user1=role1,role2,...\nuser2=role1,role2,... mapping */
    @ConfigItem
    public Map<String, String> roles;

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getUsers() {
        return users;
    }

    public void setUsers(Map<String, String> users) {
        this.users = users;
    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "MPRealmConfig{" +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                ", users=" + users +
                ", roles=" + roles +
                '}';
    }
}
