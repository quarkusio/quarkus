package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;

class AddClusterRoleResourceDecorator extends AbstractRoleResourceDecorator {
    private final List<PolicyRule> rules;

    public AddClusterRoleResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            List<PolicyRule> rules) {
        super(deploymentName, name, labels);
        this.rules = rules;
    }

    public void visit(KubernetesListBuilder list) {
        Optional<Map<String, String>> maybeRoleLabels = preVisit(list);
        if (maybeRoleLabels.isEmpty()) {
            return;
        }

        list.addToItems(new ClusterRoleBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(maybeRoleLabels.get())
                .endMetadata()
                .withRules(rules));
    }
}
