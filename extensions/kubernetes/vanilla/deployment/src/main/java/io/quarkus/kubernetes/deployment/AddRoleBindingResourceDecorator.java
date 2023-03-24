package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.ROLE;
import static io.quarkus.kubernetes.deployment.Constants.ROLE_BINDING;

import java.util.HashMap;
import java.util.Map;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;

public class AddRoleBindingResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private final String deploymentName;
    private final String name;
    private final Map<String, String> labels;
    private final KubernetesRoleBindingBuildItem.RoleRef roleRef;
    private final KubernetesRoleBindingBuildItem.Subject[] subjects;

    public AddRoleBindingResourceDecorator(String deploymentName, String name, Map<String, String> labels,
            KubernetesRoleBindingBuildItem.RoleRef roleRef,
            KubernetesRoleBindingBuildItem.Subject... subjects) {
        this.deploymentName = deploymentName;
        this.name = name;
        this.labels = labels;
        this.roleRef = roleRef;
        this.subjects = subjects;
    }

    public void visit(KubernetesListBuilder list) {
        if (contains(list, RBAC_API_VERSION, ROLE_BINDING, name)) {
            return;
        }

        Map<String, String> roleBindingLabels = new HashMap<>();
        roleBindingLabels.putAll(labels);
        getDeploymentMetadata(list, deploymentName)
                .map(ObjectMeta::getLabels)
                .ifPresent(roleBindingLabels::putAll);

        RoleBindingBuilder builder = new RoleBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(roleBindingLabels)
                .endMetadata()
                .withNewRoleRef()
                .withKind(roleRef.isClusterWide() ? CLUSTER_ROLE : ROLE)
                .withName(roleRef.getName())
                .withApiGroup(RBAC_API_GROUP)
                .endRoleRef();

        for (KubernetesRoleBindingBuildItem.Subject subject : subjects) {
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
