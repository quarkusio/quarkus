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
     * If the passwords are stored in the property file as plain text, e.g.
     * {@code quarkus.security.users.embedded.users.alice=AlicesSecretPassword}.
     * If this is false (the default) then it is expected that passwords are hashed as per the {@code algorithm} config
     * property.
     */
    @WithDefault("false")
    boolean plainText();

    /**
     * The algorithm with which user password is hashed. The library expects a password prepended with the username and the
     * realm,
     * in the form ALG( username ":" realm ":" password ) in hexadecimal format.
     * <p>
     * For example, on a Unix-like system we can produce the expected hash for Alice logging in to the Quarkus realm with
     * password AlicesSecretPassword using {@code echo -n "alice:Quarkus:AlicesSecretPassword" | sha512sum}, and thus set
     * {@code quarkus.security.users.embedded.users.alice=c8131...4546} (full hash output abbreviated here).
     * This property is ignored if {@code plainText} is true.
     */
    @WithDefault(DigestPassword.ALGORITHM_DIGEST_SHA_512)
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
