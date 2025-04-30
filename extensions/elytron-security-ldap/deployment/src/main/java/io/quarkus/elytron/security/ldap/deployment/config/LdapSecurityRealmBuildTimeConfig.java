package io.quarkus.elytron.security.ldap.deployment.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * A configuration object for an LDAP based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
 */
@ConfigMapping(prefix = "quarkus.security.ldap")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface LdapSecurityRealmBuildTimeConfig {

    /**
     * The option to enable the ldap elytron module
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The elytron realm name
     */
    @WithDefault("Quarkus")
    String realmName();

    String toString();
}
