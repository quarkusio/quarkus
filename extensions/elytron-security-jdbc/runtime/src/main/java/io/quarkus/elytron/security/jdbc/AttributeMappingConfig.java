package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.AttributeMapper}
 */
@ConfigGroup
public interface AttributeMappingConfig {

    /**
     * The index (1 based numbering) of column to map
     */
    @WithDefault("0")
    int index();

    /**
     * The target attribute name
     */
    String to();

    String toString();
}
