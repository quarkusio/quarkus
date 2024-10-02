package io.quarkus.kubernetes.spi;

import java.util.List;

/**
 * Produce this build item to request the Kubernetes extension to generate
 * a Kubernetes {@code ClusterRole} resource.
 */
public final class KubernetesClusterRoleBuildItem extends BaseTargetable {
    /**
     * Name of the generated {@code ClusterRole} resource.
     */
    private final String name;
    /**
     * The {@code PolicyRule} resources for this {@code ClusterRole}.
     */
    private final List<PolicyRule> rules;

    public KubernetesClusterRoleBuildItem(String name, List<PolicyRule> rules, String target) {
        super(target);
        this.name = name;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public List<PolicyRule> getRules() {
        return rules;
    }
}
