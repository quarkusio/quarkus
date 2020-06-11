package io.quarkus.elytron.security.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
 * }
 */
@ConfigRoot(name = "security.users.embedded")
public class MPRealmRuntimeConfig {

    /**
     * If the properties are stored in plain text. If this is false (the default) then it is expected
     * that the passwords are of the form HEX( MD5( username ":" realm ":" password ) )
     */
    @ConfigItem
    public boolean plainText;

    /**
     * The realm users user1=password\nuser2=password2... mapping.
     * See <a href="#embedded-users">Embedded Users</a>.
     */
    @ConfigItem(defaultValueDocumentation = "none")
    public Map<String, String> users;

    /**
     * The realm roles user1=role1,role2,...\nuser2=role1,role2,... mapping
     * See <a href="#embedded-roles">Embedded Roles</a>.
     */
    @ConfigItem(defaultValueDocumentation = "none")
    public Map<String, String> roles;

}
