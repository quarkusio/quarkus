package io.quarkus.kubernetes.config.runtime;

import java.util.List;
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
     * If set to true, the generated role name will be prefixed with the application name, producing a unique name per
     * application (e.g. {@code my-app-view-secrets}). This prevents name conflicts when multiple applications sharing
     * the same namespace each manage their own role.
     */
    @WithDefault("false")
    boolean generateName();

    /**
     * The names of the secrets this role is allowed to access. When specified, the generated role will only grant access
     * to the listed secrets, following the principle of least privilege. When not specified, the role grants access to
     * all secrets in the namespace.
     */
    Optional<List<String>> secretNames();
}
