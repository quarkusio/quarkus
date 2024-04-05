package io.quarkus.resteasy.reactive.common.runtime;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.container.ResourceInfo;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.security.jaxrs")
public interface JaxRsSecurityConfig {
    /**
     * if set to true, access to all JAX-RS resources will be denied by default
     */
    @WithName("deny-unannotated-endpoints")
    @WithDefault("false")
    boolean denyJaxRs();

    /**
     * If no security annotations are affecting a method then they will default to requiring these roles,
     * (equivalent to adding an @RolesAllowed annotation with the roles to every endpoint class).
     *
     * The role of '**' means any authenticated user, which is equivalent to the {@code io.quarkus.security.Authenticated}
     * annotation.
     */
    Optional<List<String>> defaultRolesAllowed();

    /**
     * Allows to run custom HTTP Security Policies after your JAX-RS resource method has been matched
     * with incoming HTTP request. Enable this option if you have custom HTTP Security Policy where you need
     * to inject {@link ResourceInfo} and perform authorization with the knowledge of the invoked resource method.
     * Please review documentation of the `quarkus.http.auth.permission."permissions".applies-to`
     * configuration property before you enable this option.
     */
    @WithName("enable-jaxrs-http-security-policies")
    @WithDefault("false")
    boolean enableJaxRsHttpSecurityPolicies();
}
