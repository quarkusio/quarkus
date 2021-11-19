package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configures a single {@code PolicyRule} for the generated {@code ClusterRole}.
 * Properties are like for like the PolicyRule attributes.
 */
@ConfigGroup
public class RolePolicyConfig {
    /**
     * Whether this policy rule should be added to a Role ({@code false})
     * or a ClusterRole ({@code true})
     */
    @ConfigItem(defaultValue = "false")
    boolean clusterWide;

    /**
     * The apiGroups for this {@code PolicyRule}
     */
    @ConfigItem
    List<String> apiGroups;

    /**
     * The nonResourceURLs for this {@code PolicyRule}
     */
    @ConfigItem
    List<String> nonResourceURLs;

    /**
     * The resourceNames for this {@code PolicyRule}
     */
    @ConfigItem
    List<String> resourceNames;

    /**
     * The resources for this {@code PolicyRule}
     */
    @ConfigItem
    List<String> resources;

    /**
     * The verbs for this {@code PolicyRule}
     */
    @ConfigItem
    List<String> verbs;
}
