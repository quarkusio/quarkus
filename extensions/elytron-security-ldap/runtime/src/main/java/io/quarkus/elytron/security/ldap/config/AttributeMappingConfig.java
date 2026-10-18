package io.quarkus.elytron.security.ldap.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.ldap.AttributeMapping}
 */
@ConfigGroup
public interface AttributeMappingConfig {

    /**
     * The roleAttributeId from which is mapped (e.g. "cn")
     */
    String from();

    /**
     * The identifier whom the attribute is mapped to (in Quarkus: "groups", in WildFly this is "Roles")
     */
    @WithDefault("groups")
    String to();

    /**
     * The filter (also named "roleFilter")
     */
    String filter();

    /**
     * The filter base dn (also named "rolesContextDn")
     */
    String filterBaseDn();
}
