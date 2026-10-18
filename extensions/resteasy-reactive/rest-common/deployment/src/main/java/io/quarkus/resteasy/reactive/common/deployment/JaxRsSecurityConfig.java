package io.quarkus.resteasy.reactive.common.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot
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
}
