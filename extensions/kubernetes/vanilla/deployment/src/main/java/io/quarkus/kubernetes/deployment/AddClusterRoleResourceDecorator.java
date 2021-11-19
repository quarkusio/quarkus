package io.quarkus.kubernetes.deployment;

import java.util.stream.Collectors;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

class AddClusterRoleResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {
    private final KubernetesRoleBuildItem spec;

    public AddClusterRoleResourceDecorator(KubernetesRoleBuildItem buildItem) {
        this.spec = buildItem;
    }

    public void visit(KubernetesListBuilder list) {
        ObjectMeta meta = getMandatoryDeploymentMetadata(list);

        if (contains(list, "rbac.authorization.k8s.io/v1", ClusterRole.class.getSimpleName(), spec.getName())) {
            return;
        }

        list.addToItems(new ClusterRoleBuilder()
                .withNewMetadata()
                .withName(spec.getName())
                .withLabels(meta.getLabels())
                .endMetadata()
                .withRules(
                        spec.getRules()
                                .stream()
                                .map(it -> new PolicyRuleBuilder()
                                        .withApiGroups(it.getApiGroups())
                                        .withNonResourceURLs(it.getNonResourceURLs())
                                        .withResourceNames(it.getResourceNames())
                                        .withResources(it.getResources())
                                        .withVerbs(it.getVerbs())
                                        .build())
                                .collect(Collectors.toList())));
    }
}
