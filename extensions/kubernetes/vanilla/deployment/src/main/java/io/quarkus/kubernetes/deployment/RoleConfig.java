package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RoleConfig {

    /**
     * The name of the role.
     */
    @ConfigItem
    Optional<String> name;

    /**
     * The namespace of the role.
     */
    @ConfigItem
    Optional<String> namespace;

    /**
     * Labels to add into the Role resource.
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    Map<String, String> labels;

    /**
     * Policy rules of the Role resource.
     */
    @ConfigItem
    Map<String, PolicyRuleConfig> policyRules;
}
