package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.ROLE;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;

class AddRoleResourceDecorator extends BaseAddRBACDecorator<Role, RoleBuilder> {
    private final String namespace;
    private final List<PolicyRule> rules;

    public AddRoleResourceDecorator(String deploymentName, String name, String namespace, Map<String, String> labels,
            List<PolicyRule> rules) {
        super(name, ROLE, deploymentName, labels);
        this.namespace = namespace;
        this.rules = rules;
    }

    @Override
    protected RoleBuilder builderWithName(String name) {
        return new RoleBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(RoleBuilder builder, Void config) {
        updateMetadata(builder.editOrNewMetadata(), namespace)
                .endMetadata()
                .withRules(rules);
    }
}
