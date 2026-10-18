package io.quarkus.elytron.security.jdbc;

import org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration information used to populate a "clear"
 * {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper}
 */
@ConfigGroup
public interface ClearPasswordMapperConfig {

    String CLEAR = "clear";

    /**
     * If the clear-password-mapper is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The index (1 based numbering) of the column containing the clear password
     */
    @WithDefault("1")
    int passwordIndex();

    default PasswordKeyMapper toPasswordKeyMapper() {
        return PasswordKeyMapper.builder()
                .setDefaultAlgorithm(CLEAR)
                .setHashColumn(passwordIndex())
                .build();
    }

    String toString();
}
