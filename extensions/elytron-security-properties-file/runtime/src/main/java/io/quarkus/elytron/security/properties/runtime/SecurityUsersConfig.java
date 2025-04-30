package io.quarkus.elytron.security.properties.runtime;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 *
 */
@ConfigMapping(prefix = "quarkus.security.users")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface SecurityUsersConfig {
    /**
     * Property Files Realm Configuration
     */
    @ConfigDocSection
    PropertiesRealmConfig file();

    /**
     * Embedded Realm Configuration
     */
    @ConfigDocSection
    MPRealmConfig embedded();

}
