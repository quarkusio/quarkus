package io.quarkus.elytron.security.ldap.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface IdentityMappingConfig {

    /**
     * The identifier which correlates to the provided user (also named "baseFilter")
     */
    @WithDefault("uid")
    String rdnIdentifier();

    /**
     * The dn where we look for users
     */
    String searchBaseDn();

    /**
     * If the child nodes are also searched for identities
     */
    @WithDefault("false")
    boolean searchRecursive();

    /**
     * The configs how we get from the attribute to the Role
     */
    Map<String, AttributeMappingConfig> attributeMappings();

    String toString();
}
