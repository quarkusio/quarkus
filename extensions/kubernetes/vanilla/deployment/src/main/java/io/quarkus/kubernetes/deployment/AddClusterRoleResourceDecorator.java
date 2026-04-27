package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;

class AddClusterRoleResourceDecorator extends BaseAddRBACDecorator<ClusterRole, ClusterRoleBuilder> {
    private final List<PolicyRule> rules;

    public AddClusterRoleResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            List<PolicyRule> rules) {
        super(name, CLUSTER_ROLE, deploymentName, labels);
        this.rules = rules;
    }

    @Override
    protected ClusterRoleBuilder builderWithName(String name) {
        return new ClusterRoleBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(ClusterRoleBuilder builder, Void config) {
        updateMetadata(builder.editOrNewMetadata(), null).endMetadata().withRules(rules);
    }
}
