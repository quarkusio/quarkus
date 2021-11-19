import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.kubernetes.api.model.ServiceAccount
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.rbac.ClusterRole
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding
import io.fabric8.kubernetes.api.model.rbac.Role
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

//Check that file exits
String basedirName = basedir
String base = basedirName.substring(basedirName.lastIndexOf('/') + 1)
File kubernetesYml = new File(basedirName, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()
kubernetesYml.withInputStream { stream ->
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null

    //Configured roles/policies and automatically created ones by kubernetes-config are present side by side
    ServiceAccount serviceAccount = list.items.find{ r -> r.kind == "ServiceAccount"}
    List<Role> roles = list.items.findAll{ r -> r.kind == "Role"}
    List<RoleBinding> roleBindings = list.items.findAll{ r -> r.kind == "RoleBinding"}
    ClusterRole clusterRole = list.items.find{ r -> r.kind == "ClusterRole"}
    ClusterRoleBinding clusterRoleBinding = list.items.find{ r -> r.kind == "ClusterRoleBinding"}
    StatefulSet app = list.items.find{ r -> r.kind == "StatefulSet"}

    String serviceAccountName = serviceAccount.metadata.name

    //Check that it contains roles and bindings named after the project
    assert serviceAccountName == base
    assert roles.size() == 2
    assert roles.find{ r -> r.metadata.name == base} != null
    assert roles.find{ r -> r.metadata.name == "view-secrets"} != null
    assert roleBindings.size() == 3
    assert roleBindings.find{b -> b.metadata.name == "$serviceAccountName-$base"} != null
    assert roleBindings.find{b -> b.metadata.name == "$base-view"} != null
    assert roleBindings.find{b -> b.metadata.name == "$base-view-secrets"} != null
    assert clusterRole != null
    assert clusterRole.metadata.name == "$base-cluster-role"
    assert clusterRoleBinding != null
    assert clusterRoleBinding.metadata.name == "$serviceAccountName-$base-cluster-role"
    assert app.spec.template.spec.serviceAccountName == serviceAccountName
}
