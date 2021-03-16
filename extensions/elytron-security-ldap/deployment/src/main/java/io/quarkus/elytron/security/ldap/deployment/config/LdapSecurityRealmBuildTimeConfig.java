package io.quarkus.elytron.security.ldap.deployment.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A configuration object for a LDAP based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
 */
@ConfigRoot(name = "security.ldap", phase = ConfigPhase.BUILD_TIME)
public class LdapSecurityRealmBuildTimeConfig {

    /**
     * The option to enable the ldap elytron module
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The elytron realm name
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    @Override
    public String toString() {
        return "LdapSecurityRealmBuildTimeConfig{" +
                "enabled=" + enabled +
                ", realmName='" + realmName + '\'' +
                '}';
    }
}
