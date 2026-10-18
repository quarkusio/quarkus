package io.quarkus.kubernetes.spi;

import java.util.List;

/**
 * Corresponds directly to the Kubernetes {@code PolicyRule} resource.
 */
public class PolicyRule {
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
