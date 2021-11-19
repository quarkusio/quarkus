package io.quarkus.kubernetes.deployment;

import io.dekorate.doc.Description;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;

@Description("Add a ClusterRoleBinding resource to the list of generated resources.")
public class AddClusterRoleBindingResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {
    private static final String DEFAULT_RBAC_API_GROUP = "rbac.authorization.k8s.io";

    private final String serviceAccount;
    private final String name;
    private final String role;

    public AddClusterRoleBindingResourceDecorator(String name, String serviceAccount, String role) {
        this.name = name;
        this.serviceAccount = serviceAccount;
        this.role = role;
    }

    public void visit(KubernetesListBuilder list) {
        ObjectMeta meta = getMandatoryDeploymentMetadata(list);
        String name = Strings.isNotNullOrEmpty(this.name) ? this.name : meta.getName() + "-" + this.role;
        String serviceAccount = Strings.isNotNullOrEmpty(this.serviceAccount) ? this.serviceAccount : meta.getName();

        if (contains(list, "rbac.authorization.k8s.io/v1", ClusterRoleBinding.class.getSimpleName(), name)) {
            return;
        }

        list.addToItems(new ClusterRoleBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(meta.getLabels())
                .endMetadata()
                .withNewRoleRef()
                .withName(role)
                .withApiGroup(DEFAULT_RBAC_API_GROUP)
                .withKind(ClusterRole.class.getSimpleName())
                .endRoleRef()
                .addNewSubject()
                .withKind(ServiceAccount.class.getSimpleName())
                .withName(serviceAccount)
                .endSubject());
    }
}
