package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;

public class AddClusterRoleBindingResourceDecorator
        extends BaseAddRBACDecorator<ClusterRoleBinding, ClusterRoleBindingBuilder> {

    private final RoleRef roleRef;
    private final Subject[] subjects;

    public AddClusterRoleBindingResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            RoleRef roleRef,
            Subject... subjects) {
        super(name, CLUSTER_ROLE_BINDING, deploymentName, labels);
        this.roleRef = roleRef;
        this.subjects = subjects;
    }

    @Override
    protected ClusterRoleBindingBuilder builderWithName(String name) {
        return new ClusterRoleBindingBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(ClusterRoleBindingBuilder builder, Void config) {
        updateMetadata(builder.editOrNewMetadata(), null).endMetadata();

        builder.withNewRoleRef()
                .withKind(CLUSTER_ROLE)
                .withName(roleRef.getName())
                .withApiGroup(RBAC_API_GROUP)
                .endRoleRef();

        for (Subject subject : subjects) {
            builder.addToSubjects(createRBACSubject(subject));
        }
    }
}
