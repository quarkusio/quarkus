package io.quarkus.kubernetes.spi;

import java.util.List;

/**
 * Produce this build item to request the Kubernetes extension to generate
 * a Kubernetes {@code Role} resource.
 * <p>
 * Note that this can't be used to generate a {@code ClusterRole}.
 */
public final class KubernetesRoleBuildItem extends BaseTargetable {
    /**
     * Name of the generated {@code Role} resource.
     */
    private final String name;
    /**
     * Namespace of the generated {@code Role} resource.
     */
    private final String namespace;
    /**
     * The {@code PolicyRule} resources for this {@code Role}.
     */
    private final List<PolicyRule> rules;

    public KubernetesRoleBuildItem(String name, List<PolicyRule> rules) {
        this(name, rules, null);
    }

    public KubernetesRoleBuildItem(String name, List<PolicyRule> rules, String target) {
        this(name, null, rules, target);
    }

    public KubernetesRoleBuildItem(String name, String namespace, List<PolicyRule> rules, String target) {
        super(target);
        this.name = name;
        this.namespace = namespace;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<PolicyRule> getRules() {
        return rules;
    }
}
