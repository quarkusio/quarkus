import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.kubernetes.api.model.ServiceAccount
import io.fabric8.kubernetes.api.model.apps.Deployment
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

    ServiceAccount serviceAccount = list.items.find{ r -> r.kind == "ServiceAccount"}
    Role role = list.items.find{ r -> r.kind == "Role"}
    RoleBinding roleBinding = list.items.find{ r -> r.kind == "RoleBinding"}
    ClusterRole clusterRole = list.items.find{ r -> r.kind == "ClusterRole"}
    ClusterRoleBinding clusterRoleBinding = list.items.find{ r -> r.kind == "ClusterRoleBinding"}
    Deployment app = list.items.find{ r -> r.kind == "Deployment"}

    String serviceAccountName = serviceAccount.metadata.name

    //Check that it contains roles and bindings named after the project
    assert serviceAccountName == base
    assert role != null
    assert role.metadata.name == base
    assert roleBinding != null
    assert roleBinding.metadata.name == "$serviceAccountName-$base"
    assert clusterRole != null
    assert clusterRole.metadata.name == "$base-cluster-role"
    assert clusterRoleBinding != null
    assert clusterRoleBinding.metadata.name == "$serviceAccountName-$base-cluster-role"
    assert app.spec.template.spec.serviceAccountName == serviceAccountName
}
