package org.jboss.shamrock.security;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

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
