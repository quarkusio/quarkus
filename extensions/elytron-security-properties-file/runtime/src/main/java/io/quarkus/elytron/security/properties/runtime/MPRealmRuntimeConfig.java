package io.quarkus.elytron.security.properties.runtime;

import java.util.Map;

import org.wildfly.security.password.interfaces.DigestPassword;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
 * }
 */
@ConfigMapping(prefix = "quarkus.security.users.embedded")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MPRealmRuntimeConfig {

    /**
     * If the properties are stored in plain text. If this is false (the default) then it is expected
     * that the passwords are of the form HEX( MD5( username ":" realm ":" password ) )
     */
    @WithDefault("false")
    boolean plainText();

    /**
     * Determine which algorithm to use.
     * <p>
     * This property is ignored if {@code plainText} is true.
     */
    @WithDefault(DigestPassword.ALGORITHM_DIGEST_MD5)
    DigestAlgorithm algorithm();

    /**
     * The realm users user1=password\nuser2=password2... mapping.
     * See <a href="#embedded-users">Embedded Users</a>.
     */
    @ConfigDocDefault("none")
    Map<@WithConverter(TrimmedStringConverter.class) String, @WithConverter(TrimmedStringConverter.class) String> users();

    /**
     * The realm roles user1=role1,role2,...\nuser2=role1,role2,... mapping
     * See <a href="#embedded-roles">Embedded Roles</a>.
     */
    @ConfigDocDefault("none")
    Map<@WithConverter(TrimmedStringConverter.class) String, @WithConverter(TrimmedStringConverter.class) String> roles();

}
