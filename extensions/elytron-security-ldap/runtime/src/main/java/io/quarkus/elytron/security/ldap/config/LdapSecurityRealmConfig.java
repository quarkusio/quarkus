package io.quarkus.elytron.security.ldap.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A configuration object for a jdbc based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
 */
@ConfigRoot(name = "security.ldap", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LdapSecurityRealmConfig {

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

    /**
     * Provided credentials are verified against ldap?
     */
    @ConfigItem(defaultValue = "true")
    public boolean directVerification;

    /**
     * The ldap server configuration
     */
    @ConfigItem
    public DirContextConfig dirContext;

    /**
     * The config which we use to map an identity
     */
    @ConfigItem
    public IdentityMappingConfig identityMapping;

    @Override
    public String toString() {
        return "LdapSecurityRealmConfig{" +
                "enabled=" + enabled +
                ", realmName='" + realmName + '\'' +
                ", directVerification=" + directVerification +
                ", dirContext=" + dirContext +
                ", identityMapping=" + identityMapping +
                '}';
    }
}
