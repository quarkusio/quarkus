package io.quarkus.kubernetes.deployment;

import java.util.stream.Collectors;

import io.dekorate.deps.kubernetes.api.model.KubernetesListBuilder;
import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.dekorate.deps.kubernetes.api.model.rbac.RoleBuilder;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

class AddRoleResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {
    private final KubernetesRoleBuildItem spec;

    public AddRoleResourceDecorator(KubernetesRoleBuildItem buildItem) {
        this.spec = buildItem;
    }

    public void visit(KubernetesListBuilder list) {
        ObjectMeta meta = getMandatoryDeploymentMetadata(list);

        if (contains(list, "rbac.authorization.k8s.io/v1", "Role", spec.getName())) {
            return;
        }

        list.addToItems(new RoleBuilder()
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
