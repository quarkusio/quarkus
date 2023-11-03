package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RbacConfig {
    /**
     * List of roles to generate.
     */
    @ConfigItem
    Map<String, RoleConfig> roles;

    /**
     * List of cluster roles to generate.
     */
    @ConfigItem
    Map<String, ClusterRoleConfig> clusterRoles;

    /**
     * List of service account resources to generate.
     */
    @ConfigItem
    Map<String, ServiceAccountConfig> serviceAccounts;

    /**
     * List of role bindings to generate.
     */
    @ConfigItem
    Map<String, RoleBindingConfig> roleBindings;

    /**
     * List of cluster role bindings to generate.
     */
    @ConfigItem
    Map<String, ClusterRoleBindingConfig> clusterRoleBindings;
}
