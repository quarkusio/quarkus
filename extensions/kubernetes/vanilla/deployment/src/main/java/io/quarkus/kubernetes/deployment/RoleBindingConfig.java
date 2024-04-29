package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RoleBindingConfig {

    /**
     * Name of the RoleBinding resource to be generated. If not provided, it will use the application name plus the role
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
     * The name of the Role resource to use by the RoleRef element in the generated Role Binding resource.
     * By default, it's "view" role name.
     */
    @ConfigItem
    public Optional<String> roleName;

    /**
     * If the Role sets in the `role-name` property is cluster wide or not.
     */
    @ConfigItem
    public Optional<Boolean> clusterWide;

    /**
     * List of subjects elements to use in the generated RoleBinding resource.
     */
    @ConfigItem
    public Map<String, SubjectConfig> subjects;
}
