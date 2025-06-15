package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_VERSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;

class AddClusterRoleResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {
    private final String deploymentName;
    private final String name;
    private final Map<String, String> labels;
    private final List<PolicyRule> rules;

    public AddClusterRoleResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            List<PolicyRule> rules) {
        this.deploymentName = deploymentName;
        this.name = name;
        this.labels = labels;
        this.rules = rules;
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        if (contains(list, RBAC_API_VERSION, CLUSTER_ROLE, name)) {
            return;
        }

        Map<String, String> roleLabels = new HashMap<>();
        roleLabels.putAll(labels);
        getDeploymentMetadata(list, deploymentName)
                .map(ObjectMeta::getLabels)
                .ifPresent(roleLabels::putAll);

        list.addToItems(new ClusterRoleBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(roleLabels)
                .endMetadata()
                .withRules(rules));
    }
}
