package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ClusterRoleConfig {

    /**
     * The name of the cluster role.
     */
    @ConfigItem
    Optional<String> name;

    /**
     * Labels to add into the ClusterRole resource.
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    Map<String, String> labels;

    /**
     * Policy rules of the ClusterRole resource.
     */
    @ConfigItem
    Map<String, PolicyRuleConfig> policyRules;
}
