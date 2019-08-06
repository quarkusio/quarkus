package io.quarkus.elytron.security.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class SecurityConfig {
    /**
     * The configuration for the `org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm`
     */
    @ConfigItem
    public PropertiesRealmConfig file;
    /**
     * The configuration for the `org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm`
     */
    @ConfigItem
    public MPRealmConfig embedded;

    /**
     * List of security providers to enable for reflection
     */
    @ConfigItem
    public List<String> securityProviders;
}
