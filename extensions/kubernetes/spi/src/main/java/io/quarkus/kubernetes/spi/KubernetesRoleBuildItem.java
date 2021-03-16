package io.quarkus.kubernetes.spi;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produce this build item to request the Kubernetes extension to generate
 * a Kubernetes {@code Role} resource.
 * <p>
 * Note that this can't be used to generate a {@code ClusterRole}.
 */
public final class KubernetesRoleBuildItem extends MultiBuildItem {
    /**
     * Name of the generated {@code Role} resource.
     */
    private final String name;
    /**
     * The {@code PolicyRule} resources for this {@code Role}.
     */
    private final List<PolicyRule> rules;

    public KubernetesRoleBuildItem(String name, List<PolicyRule> rules) {
        this.name = name;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public List<PolicyRule> getRules() {
        return rules;
    }

    /**
     * Corresponds directly to the Kubernetes {@code PolicyRule} resource.
     */
    public static final class PolicyRule {
        private final List<String> apiGroups;
        private final List<String> nonResourceURLs;
        private final List<String> resourceNames;
        private final List<String> resources;
        private final List<String> verbs;

        public PolicyRule(List<String> apiGroups, List<String> resources, List<String> verbs) {
            this(apiGroups, null, null, resources, verbs);
        }

        public PolicyRule(List<String> apiGroups, List<String> nonResourceURLs, List<String> resourceNames,
                List<String> resources, List<String> verbs) {
            this.apiGroups = apiGroups;
            this.nonResourceURLs = nonResourceURLs;
            this.resourceNames = resourceNames;
            this.resources = resources;
            this.verbs = verbs;
        }

        public List<String> getApiGroups() {
            return apiGroups;
        }

        public List<String> getNonResourceURLs() {
            return nonResourceURLs;
        }

        public List<String> getResourceNames() {
            return resourceNames;
        }

        public List<String> getResources() {
            return resources;
        }

        public List<String> getVerbs() {
            return verbs;
        }
    }
}
