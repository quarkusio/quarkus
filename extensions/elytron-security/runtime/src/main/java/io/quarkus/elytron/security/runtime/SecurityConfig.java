package io.quarkus.elytron.security.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class SecurityConfig {
    /**
     * The configuration for the {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
     */
    @ConfigItem
    public PropertiesRealmConfig file;
    /**
     * The configuration for the {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
     */
    @ConfigItem
    public MPRealmConfig embedded;
}
