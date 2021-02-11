package io.quarkus.elytron.security.jdbc;

import org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper;

/**
 * Convert the current object into an instance of {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper}
 */
public interface PasswordKeyMapperConfig {

    PasswordKeyMapper toPasswordKeyMapper();
}
