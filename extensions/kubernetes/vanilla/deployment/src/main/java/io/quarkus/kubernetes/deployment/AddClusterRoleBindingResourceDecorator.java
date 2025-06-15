package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;
import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE_BINDING;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_VERSION;

import java.util.HashMap;
import java.util.Map;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;

public class AddClusterRoleBindingResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private final String deploymentName;
    private final String name;
    private final Map<String, String> labels;
    private final RoleRef roleRef;
    private final Subject[] subjects;

    public AddClusterRoleBindingResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            RoleRef roleRef, Subject... subjects) {
        this.deploymentName = deploymentName;
        this.name = name;
        this.labels = labels;
        this.roleRef = roleRef;
        this.subjects = subjects;
    }

    public void visit(KubernetesListBuilder list) {
        if (contains(list, RBAC_API_VERSION, CLUSTER_ROLE_BINDING, name)) {
            return;
        }

        Map<String, String> clusterRoleBindingLabels = new HashMap<>();
        clusterRoleBindingLabels.putAll(labels);
        getDeploymentMetadata(list, deploymentName).map(ObjectMeta::getLabels)
                .ifPresent(clusterRoleBindingLabels::putAll);

        ClusterRoleBindingBuilder builder = new ClusterRoleBindingBuilder().withNewMetadata().withName(name)
                .withLabels(clusterRoleBindingLabels).endMetadata().withNewRoleRef().withKind(CLUSTER_ROLE)
                .withName(roleRef.getName()).withApiGroup(RBAC_API_GROUP).endRoleRef();

        for (Subject subject : subjects) {
            builder.addNewSubject().withApiGroup(subject.getApiGroup()).withKind(subject.getKind())
                    .withName(Strings.defaultIfEmpty(subject.getName(), deploymentName))
                    .withNamespace(subject.getNamespace()).endSubject();
        }

        list.addToItems(builder.build());
    }
}
