package org.jboss.shamrock.jwt.deployment;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 * deployment configuration
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME_STATIC, name = "jwt")
public class JWTAuthContextInfoGroup {

    /**
     * The authentication mechanism
     */
    @ConfigItem(name = "authMechanism", defaultValue = "MP-JWT")
    public String authMechanism;

    /**
     * The authentication mechanism
     */
    @ConfigItem(name = "realmName", defaultValue = "Shamrock-JWT")
    public String realmName;

    /**
     * The MP-JWT configuration object
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;
}
