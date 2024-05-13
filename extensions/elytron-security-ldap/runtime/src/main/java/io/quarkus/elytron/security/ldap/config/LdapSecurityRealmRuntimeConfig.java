package io.quarkus.elytron.security.ldap.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration object for an LDAP based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
 */
@ConfigMapping(prefix = "quarkus.security.ldap")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LdapSecurityRealmRuntimeConfig {

    /**
     * Provided credentials are verified against ldap?
     */
    @WithDefault("true")
    boolean directVerification();

    /**
     * The ldap server configuration
     */
    DirContextConfig dirContext();

    /**
     * The LDAP cache configuration
     */
    CacheConfig cache();

    /**
     * The config which we use to map an identity
     */
    IdentityMappingConfig identityMapping();

    String toString();
}
