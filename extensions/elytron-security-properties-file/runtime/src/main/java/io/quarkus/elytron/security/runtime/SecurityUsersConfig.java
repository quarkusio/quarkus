package io.quarkus.elytron.security.runtime;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(name = "security.users", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class SecurityUsersConfig {
    /**
     * Property Files Realm Configuration
     */
    @ConfigItem
    @ConfigDocSection
    public PropertiesRealmConfig file;
    /**
     * Embedded Realm Configuration
     */
    @ConfigItem
    @ConfigDocSection
    public MPRealmConfig embedded;

}
