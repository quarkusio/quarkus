package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.quarkus.kubernetes.spi.PolicyRule;

public class RBACUtil {
    private static final List<String> LIST_WITH_EMPTY = List.of("");

    private RBACUtil() {
    }

    public static io.fabric8.kubernetes.api.model.rbac.PolicyRule from(PolicyRule rule) {
        if (rule == null) {
            return null;
        }
        return new PolicyRuleBuilder()
                .withApiGroups(rule.getApiGroups())
                .withNonResourceURLs(rule.getNonResourceURLs())
                .withResourceNames(rule.getResourceNames())
                .withResources(rule.getResources())
                .withVerbs(rule.getVerbs())
                .build();
    }

    public static io.fabric8.kubernetes.api.model.rbac.PolicyRule from(PolicyRuleConfig policyRuleConfig) {
        if (policyRuleConfig == null) {
            return null;
        }
        return new PolicyRuleBuilder()
                .withApiGroups(policyRuleConfig.apiGroups.orElse(LIST_WITH_EMPTY))
                .withNonResourceURLs(policyRuleConfig.nonResourceUrls.orElse(null))
                .withResourceNames(policyRuleConfig.resourceNames.orElse(null))
                .withResources(policyRuleConfig.resources.orElse(null))
                .withVerbs(policyRuleConfig.verbs.orElse(null))
                .build();
    }
}
