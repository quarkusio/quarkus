package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;

class AddRoleResourceDecorator extends AbstractRoleResourceDecorator {
    private final String namespace;
    private final List<PolicyRule> rules;

    public AddRoleResourceDecorator(String deploymentName, String name, String namespace, Map<String, String> labels,
            List<PolicyRule> rules) {
        super(deploymentName, name, labels);
        this.namespace = namespace;
        this.rules = rules;
    }

    public void visit(KubernetesListBuilder list) {
        Optional<Map<String, String>> maybeRoleLabels = preVisit(list);
        if (maybeRoleLabels.isEmpty()) {
            return;
        }

        list.addToItems(new RoleBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(maybeRoleLabels.get())
                .endMetadata()
                .withRules(rules));
    }
}
