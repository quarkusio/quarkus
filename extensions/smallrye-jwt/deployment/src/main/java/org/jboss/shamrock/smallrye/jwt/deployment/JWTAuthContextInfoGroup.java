package org.jboss.shamrock.smallrye.jwt.deployment;

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
    @ConfigItem(defaultValue = "MP-JWT")
    public String authMechanism;

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "Shamrock-JWT")
    public String realmName;

    /**
     * The MP-JWT configuration object
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled = true;
}
