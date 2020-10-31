package io.quarkus.elytron.security.ldap.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class IdentityMappingConfig {

    /**
     * The identifier which correlates to the provided user (also named "baseFilter")
     */
    @ConfigItem(defaultValue = "uid")
    public String rdnIdentifier;

    /**
     * The dn where we look for users
     */
    @ConfigItem
    public String searchBaseDn;

    /**
     * If the child nodes are also searched for identities
     */
    @ConfigItem(defaultValue = "false")
    public boolean searchRecursive;

    /**
     * The configs how we get from the attribute to the Role
     */
    @ConfigItem
    public Map<String, AttributeMappingConfig> attributeMappings;

    @Override
    public String toString() {
        return "IdentityMappingConfig{" +
                "rdnIdentifier='" + rdnIdentifier + '\'' +
                ", searchBaseDn='" + searchBaseDn + '\'' +
                ", searchRecursive=" + searchRecursive +
                ", attributeMappings=" + attributeMappings +
                '}';
    }
}
