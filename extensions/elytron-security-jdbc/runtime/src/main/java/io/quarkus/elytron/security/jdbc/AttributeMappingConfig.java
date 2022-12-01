package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.AttributeMapper}
 */
@ConfigGroup
public class AttributeMappingConfig {

    /**
     * The index (1 based numbering) of column to map
     */
    @ConfigItem
    public int index;

    /**
     * The target attribute name
     */
    @ConfigItem
    public String to;

    @Override
    public String toString() {
        return "AttributeMappingConfig{" +
                "index=" + index +
                ", to='" + to + '\'' +
                '}';
    }
}
