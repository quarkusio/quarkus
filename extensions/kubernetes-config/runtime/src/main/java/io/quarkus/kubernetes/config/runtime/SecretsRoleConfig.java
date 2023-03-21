package io.quarkus.kubernetes.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SecretsRoleConfig {

    /**
     * The name of the role.
     */
    @ConfigItem(defaultValue = "view-secrets")
    public String name;

    /**
     * The namespace of the role.
     */
    @ConfigItem
    public Optional<String> namespace;

    /**
     * Whether the role is cluster wide or not. By default, it's not a cluster wide role.
     */
    @ConfigItem(defaultValue = "false")
    public boolean clusterWide;

    /**
     * If the current role is meant to be generated or not. If not, it will only be used to generate the role binding resource.
     */
    @ConfigItem(defaultValue = "true")
    public boolean generate;
}
