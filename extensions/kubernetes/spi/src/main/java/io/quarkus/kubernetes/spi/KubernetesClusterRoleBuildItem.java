package io.quarkus.kubernetes.spi;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produce this build item to request the Kubernetes extension to generate
 * a Kubernetes {@code ClusterRole} resource.
 */
public final class KubernetesClusterRoleBuildItem extends MultiBuildItem {
    /**
     * Name of the generated {@code ClusterRole} resource.
     */
    private final String name;
    /**
     * The {@code PolicyRule} resources for this {@code ClusterRole}.
     */
    private final List<PolicyRule> rules;

    /**
     * The target manifest that should include this role.
     */
    private final String target;

    public KubernetesClusterRoleBuildItem(String name, List<PolicyRule> rules, String target) {
        this.name = name;
        this.rules = rules;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public List<PolicyRule> getRules() {
        return rules;
    }

    public String getTarget() {
        return target;
    }
}
