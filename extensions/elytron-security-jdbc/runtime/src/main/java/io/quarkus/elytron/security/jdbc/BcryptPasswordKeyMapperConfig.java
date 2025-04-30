package io.quarkus.elytron.security.jdbc;

import org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper;
import org.wildfly.security.password.spec.Encoding;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration information used to populate a "bcrypt"
 * {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper}
 */
@ConfigGroup
public interface BcryptPasswordKeyMapperConfig {

    String BCRYPT = "bcrypt";

    /**
     * If the bcrypt-password-mapper is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The index (1 based numbering) of the column containing the password hash
     */
    @WithDefault("0")
    int passwordIndex();

    /**
     * A string referencing the password hash encoding ("BASE64" or "HEX")
     */
    @WithDefault("BASE64")
    Encoding hashEncoding();

    /**
     * The index (1 based numbering) of the column containing the Bcrypt salt. The default value of `-1` implies that the salt
     * is stored in the password column using the Modular Crypt Format (MCF) standard.
     */
    @WithDefault("-1")
    int saltIndex();

    /**
     * A string referencing the salt encoding ("BASE64" or "HEX")
     */
    @WithDefault("BASE64")
    Encoding saltEncoding();

    /**
     * The index (1 based numbering) of the column containing the Bcrypt iteration count. The default value of `-1` implies that
     * the iteration count is stored in the password column using the Modular Crypt Format (MCF) standard.
     */
    @WithDefault("-1")
    int iterationCountIndex();

    default PasswordKeyMapper toPasswordKeyMapper() {
        return PasswordKeyMapper.builder()
                .setDefaultAlgorithm(BCRYPT)
                .setHashColumn(passwordIndex())
                .setHashEncoding(hashEncoding())
                .setSaltColumn(saltIndex())
                .setSaltEncoding(saltEncoding())
                .setIterationCountColumn(iterationCountIndex())
                .build();
    }

    String toString();
}
