package io.quarkus.resteasy.runtime;

import java.util.List;
import java.util.Optional;

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

    /**
     * If no security annotations are affecting a method then they will default to requiring these roles,
     * (equivalent to adding an @RolesAllowed annotation with the roles to every endpoint class).
     *
     * The role of '**' means any authenticated user, which is equivalent to the {@link io.quarkus.security.Authenticated}
     * annotation.
     *
     */
    @ConfigItem
    public Optional<List<String>> defaultRolesAllowed;

}
