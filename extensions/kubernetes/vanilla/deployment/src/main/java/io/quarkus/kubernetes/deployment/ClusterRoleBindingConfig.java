package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ClusterRoleBindingConfig {

    /**
     * Name of the ClusterRoleBinding resource to be generated. If not provided, it will use the application name plus the role
     * ref name.
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * Labels to add into the RoleBinding resource.
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    public Map<String, String> labels;

    /**
     * The name of the ClusterRole resource to use by the RoleRef element in the generated ClusterRoleBinding resource.
     */
    @ConfigItem
    public String roleName;

    /**
     * List of subjects elements to use in the generated ClusterRoleBinding resource.
     */
    @ConfigItem
    public Map<String, SubjectConfig> subjects;
}
