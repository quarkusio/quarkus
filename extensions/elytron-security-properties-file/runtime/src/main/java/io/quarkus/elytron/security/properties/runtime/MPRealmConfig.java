package io.quarkus.elytron.security.properties.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration information used to populate a {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
 * }
 */
@ConfigGroup
public interface MPRealmConfig {

    /**
     * The realm name. This is used when generating a hashed password
     */
    @WithDefault("Quarkus")
    String realmName();

    /**
     * Determine whether security via the embedded realm is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    String toString();
}
