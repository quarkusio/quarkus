package io.quarkus.elytron.security.ldap.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Runtime configuration object for a LDAP based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
 */
@ConfigRoot(name = "security.ldap", phase = ConfigPhase.RUN_TIME)
public class LdapSecurityRealmRuntimeConfig {

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
        return "LdapSecurityRealmRuntimeConfig{" +
                "directVerification=" + directVerification +
                ", dirContext=" + dirContext +
                ", identityMapping=" + identityMapping +
                '}';
    }
}
