package io.quarkus.elytron.security.jdbc;

import org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a "clear"
 * {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper}
 */
@ConfigGroup
public class ClearPasswordMapperConfig implements PasswordKeyMapperConfig {

    public static final String CLEAR = "clear";

    /**
     * If the clear-password-mapper is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The index (1 based numbering) of the column containing the clear password
     */
    @ConfigItem(defaultValue = "1")
    public int passwordIndex;

    @Override
    public PasswordKeyMapper toPasswordKeyMapper() {
        return PasswordKeyMapper.builder()
                .setDefaultAlgorithm(CLEAR)
                .setHashColumn(passwordIndex)
                .build();
    }

    @Override
    public String toString() {
        return "ClearPasswordMapperConfig{" +
                "enabled=" + enabled +
                ", passwordIndex=" + passwordIndex +
                '}';
    }
}
