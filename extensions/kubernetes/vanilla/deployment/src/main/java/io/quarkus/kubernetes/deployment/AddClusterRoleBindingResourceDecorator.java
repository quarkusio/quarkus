package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_GROUP;

import java.util.Map;
import java.util.Optional;

import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;

public class AddClusterRoleBindingResourceDecorator extends AbstractRoleResourceDecorator {

    private final RoleRef roleRef;
    private final Subject[] subjects;

    public AddClusterRoleBindingResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            RoleRef roleRef,
            Subject... subjects) {
        super(deploymentName, name, labels);
        this.roleRef = roleRef;
        this.subjects = subjects;
    }

    public void visit(KubernetesListBuilder list) {
        Optional<Map<String, String>> maybeClusterRoleBindingLabels = preVisit(list);
        if (maybeClusterRoleBindingLabels.isEmpty()) {
            return;
        }

        ClusterRoleBindingBuilder builder = new ClusterRoleBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(maybeClusterRoleBindingLabels.get())
                .endMetadata()
                .withNewRoleRef()
                .withKind(CLUSTER_ROLE)
                .withName(roleRef.getName())
                .withApiGroup(RBAC_API_GROUP)
                .endRoleRef();

        for (Subject subject : subjects) {
            builder.addNewSubject()
                    .withApiGroup(subject.getApiGroup())
                    .withKind(subject.getKind())
                    .withName(Strings.defaultIfEmpty(subject.getName(), deploymentName))
                    .withNamespace(subject.getNamespace())
                    .endSubject();
        }

        list.addToItems(builder.build());
    }
}
