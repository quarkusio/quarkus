package io.quarkus.elytron.security.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
 * }
 */
@ConfigGroup
public class MPRealmConfig {

    /**
     * The realm name. This is used when generating a hashed password
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * Determine whether security via the embedded realm is enabled.
     */
    @ConfigItem
    public boolean enabled;

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

    @Override
    public String toString() {
        return "MPRealmConfig{" +
                "realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
