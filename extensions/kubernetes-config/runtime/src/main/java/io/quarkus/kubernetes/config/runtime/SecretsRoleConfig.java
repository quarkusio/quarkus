package io.quarkus.kubernetes.config.runtime;

import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface SecretsRoleConfig {

    /**
     * The name of the role.
     */
    @WithDefault("view-secrets")
    String name();

    /**
     * The namespace of the role.
     */
    Optional<String> namespace();

    /**
     * Whether the role is cluster wide or not. By default, it's not a cluster wide role.
     */
    @WithDefault("false")
    boolean clusterWide();

    /**
     * If the current role is meant to be generated or not. If not, it will only be used to generate the role binding resource.
     */
    @WithDefault("true")
    boolean generate();

    /**
     * If set to true, the role name is prefixed with the application name, producing a unique name per application
     * (e.g. {@code my-app-view-secrets}). This prevents name conflicts when multiple applications sharing the same
     * namespace each manage their own role.
     */
    @WithDefault("false")
    boolean prefixName();
}
