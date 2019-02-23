package io.quarkus.security;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
final class SecurityConfig {
    /**
     * The configuration for the {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
     */
    @ConfigItem
    PropertiesRealmConfig file;
    /**
     * The configuration for the {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
     */
    @ConfigItem
    MPRealmConfig embedded;
}
