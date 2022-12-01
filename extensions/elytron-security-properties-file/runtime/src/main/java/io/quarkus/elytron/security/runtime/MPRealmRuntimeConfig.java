package io.quarkus.elytron.security.runtime;

import java.util.Map;

import org.wildfly.security.password.interfaces.DigestPassword;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
 * }
 */
@ConfigRoot(name = "security.users.embedded", phase = ConfigPhase.RUN_TIME)
public class MPRealmRuntimeConfig {

    /**
     * If the properties are stored in plain text. If this is false (the default) then it is expected
     * that the passwords are of the form HEX( MD5( username ":" realm ":" password ) )
     */
    @ConfigItem
    public boolean plainText;

    /**
     * Determine which algorithm to use.
     * <p>
     * This property is ignored if {@code plainText} is true.
     */
    @ConfigItem(defaultValue = DigestPassword.ALGORITHM_DIGEST_MD5)
    public DigestAlgorithm algorithm;

    /**
     * The realm users user1=password\nuser2=password2... mapping.
     * See <a href="#embedded-users">Embedded Users</a>.
     */
    @ConfigItem(defaultValueDocumentation = "none")
    @ConvertWith(TrimmedStringConverter.class)
    public Map<String, String> users;

    /**
     * The realm roles user1=role1,role2,...\nuser2=role1,role2,... mapping
     * See <a href="#embedded-roles">Embedded Roles</a>.
     */
    @ConfigItem(defaultValueDocumentation = "none")
    @ConvertWith(TrimmedStringConverter.class)
    public Map<String, String> roles;

}
