package io.quarkus.resteasy.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ConfigRoot(name = "security.jaxrs", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JaxRsSecurityConfig {
    /**
     * if set to true, access to all JAX-RS resources will be denied by default
     */
    @ConfigItem(name = "deny-unannotated-endpoints")
    public boolean denyJaxRs;
}
