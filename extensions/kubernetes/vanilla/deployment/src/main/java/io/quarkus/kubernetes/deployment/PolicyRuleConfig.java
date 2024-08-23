package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PolicyRuleConfig {
    /**
     * API groups of the policy rule.
     */
    @ConfigItem
    Optional<List<String>> apiGroups;

    /**
     * Non resource URLs of the policy rule.
     */
    @ConfigItem
    Optional<List<String>> nonResourceUrls;

    /**
     * Resource names of the policy rule.
     */
    @ConfigItem
    Optional<List<String>> resourceNames;

    /**
     * Resources of the policy rule.
     */
    @ConfigItem
    Optional<List<String>> resources;

    /**
     * Verbs of the policy rule.
     */
    @ConfigItem
    Optional<List<String>> verbs;
}
