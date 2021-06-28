package io.quarkus.elytron.security.jdbc;

import org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper;
import org.wildfly.security.password.spec.Encoding;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration information used to populate a "bcrypt"
 * {@linkplain org.wildfly.security.auth.realm.jdbc.mapper.PasswordKeyMapper}
 */
@ConfigGroup
public class BcryptPasswordKeyMapperConfig implements PasswordKeyMapperConfig {

    public static final String BCRYPT = "bcrypt";

    /**
     * If the bcrypt-password-mapper is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The index (1 based numbering) of the column containing the password hash
     */
    @ConfigItem
    public int passwordIndex;

    /**
     * A string referencing the password hash encoding ("BASE64" or "HEX")
     */
    @ConfigItem(defaultValue = "BASE64")
    public Encoding hashEncoding;

    /**
     * The index (1 based numbering) of the column containing the Bcrypt salt
     */
    @ConfigItem
    public int saltIndex;

    /**
     * A string referencing the salt encoding ("BASE64" or "HEX")
     */
    @ConfigItem(defaultValue = "BASE64")
    public Encoding saltEncoding;

    /**
     * The index (1 based numbering) of the column containing the Bcrypt iteration count
     */
    @ConfigItem
    public int iterationCountIndex;

    @Override
    public PasswordKeyMapper toPasswordKeyMapper() {
        return PasswordKeyMapper.builder()
                .setDefaultAlgorithm(BCRYPT)
                .setHashColumn(passwordIndex)
                .setHashEncoding(hashEncoding)
                .setSaltColumn(saltIndex)
                .setSaltEncoding(saltEncoding)
                .setIterationCountColumn(iterationCountIndex)
                .build();
    }

    @Override
    public String toString() {
        return "BcryptPasswordKeyMapperConfig{" +
                "enabled=" + enabled +
                ", passwordIndex=" + passwordIndex +
                ", hashEncoding=" + hashEncoding +
                ", saltIndex=" + saltIndex +
                ", saltEncoding=" + saltEncoding +
                ", iterationCountIndex=" + iterationCountIndex +
                '}';
    }
}
