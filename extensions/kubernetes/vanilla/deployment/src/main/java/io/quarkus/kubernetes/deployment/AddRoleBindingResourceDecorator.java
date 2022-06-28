package io.quarkus.kubernetes.deployment;

import java.util.Collections;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;

/**
 * Workaround for: https://github.com/dekorateio/dekorate/issues/987
 * Once the issue is fixed in upstream, and we bump the Dekorate version, we should delete this decorator.
 */
public class AddRoleBindingResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private static final String DEFAULT_RBAC_API_GROUP = "rbac.authorization.k8s.io";

    public static enum RoleKind {
        Role,
        ClusterRole
    }

    private final String serviceAccount;
    private final String name;
    private final String role;
    private final RoleKind kind;

    public AddRoleBindingResourceDecorator(String name, String serviceAccount, String role, RoleKind kind) {
        this.name = name;
        this.serviceAccount = serviceAccount;
        this.role = role;
        this.kind = kind;
    }

    public void visit(KubernetesListBuilder list) {
        // If name is null, it will get the first deployment resource.
        ObjectMeta meta = getMandatoryDeploymentMetadata(list, name);
        String roleBindingName = meta.getName() + "-" + this.role;
        String serviceAccount = Strings.isNotNullOrEmpty(this.serviceAccount) ? this.serviceAccount : meta.getName();

        if (contains(list, "rbac.authorization.k8s.io/v1", "RoleBinding", roleBindingName)) {
            return;
        }

        list.addToItems(new RoleBindingBuilder()
                .withNewMetadata()
                .withName(roleBindingName)
                .withLabels(Strings.isNotNullOrEmpty(name) ? getMandatoryDeploymentMetadata(list, name).getLabels()
                        : Collections.emptyMap())
                .endMetadata()
                .withNewRoleRef()
                .withKind(kind.name())
                .withName(role)
                .withApiGroup(DEFAULT_RBAC_API_GROUP)
                .endRoleRef()
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName(serviceAccount)
                .endSubject());
    }
}
