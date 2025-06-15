package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.ROLE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;

class AddRoleResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {
    private final String deploymentName;
    private final String name;
    private final String namespace;
    private final Map<String, String> labels;
    private final List<PolicyRule> rules;

    public AddRoleResourceDecorator(String deploymentName, String name, String namespace, Map<String, String> labels,
            List<PolicyRule> rules) {
        this.deploymentName = deploymentName;
        this.name = name;
        this.namespace = namespace;
        this.labels = labels;
        this.rules = rules;
    }

    public void visit(KubernetesListBuilder list) {
        if (contains(list, RBAC_API_VERSION, ROLE, name)) {
            return;
        }

        Map<String, String> roleLabels = new HashMap<>();
        roleLabels.putAll(labels);
        getDeploymentMetadata(list, deploymentName).map(ObjectMeta::getLabels).ifPresent(roleLabels::putAll);

        list.addToItems(new RoleBuilder().withNewMetadata().withName(name).withNamespace(namespace)
                .withLabels(roleLabels).endMetadata().withRules(rules));
    }
}
