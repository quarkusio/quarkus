package io.quarkus.elytron.security.ldap.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.ldap.AttributeMapping}
 */
@ConfigGroup
public class AttributeMappingConfig {

    /**
     * The roleAttributeId from which is mapped (e.g. "cn")
     */
    @ConfigItem
    public String from;

    /**
     * The identifier whom the attribute is mapped to (in Quarkus: "groups", in WildFly this is "Roles")
     */
    @ConfigItem(defaultValue = "groups")
    public String to;

    /**
     * The filter (also named "roleFilter")
     */
    @ConfigItem
    public String filter;

    /**
     * The filter base dn (also named "rolesContextDn")
     */
    @ConfigItem
    public String filterBaseDn;
}
