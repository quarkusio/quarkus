package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.ROLE;
import static io.quarkus.kubernetes.deployment.Constants.ROLE_BINDING;

import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;

public class AddRoleBindingResourceDecorator extends BaseAddRBACDecorator<RoleBinding, RoleBindingBuilder> {

    private final String namespace;
    private final RoleRef roleRef;
    private final Subject[] subjects;

    public AddRoleBindingResourceDecorator(String deploymentName, String name, String namespace, Map<String, String> labels,
            RoleRef roleRef,
            Subject... subjects) {
        super(name, ROLE_BINDING, deploymentName, labels);
        this.roleRef = roleRef;
        this.subjects = subjects;
        this.namespace = namespace;
    }

    @Override
    protected RoleBindingBuilder builderWithName(String name) {
        return new RoleBindingBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(RoleBindingBuilder builder, Void config) {
        updateMetadata(builder.editOrNewMetadata(), namespace).endMetadata();

        builder.withNewRoleRef()
                .withKind(roleRef.isClusterWide() ? CLUSTER_ROLE : ROLE)
                .withName(roleRef.getName())
                .withApiGroup(RBAC_API_GROUP)
                .endRoleRef();

        for (Subject subject : subjects) {
            builder.addToSubjects(createRBACSubject(subject));
        }
    }
}
