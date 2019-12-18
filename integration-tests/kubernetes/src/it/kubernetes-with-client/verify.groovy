import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.kubernetes.api.model.ServiceAccount
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;
import io.dekorate.deps.kubernetes.api.model.rbac.RoleBinding;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()
kubernetesYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    Deployment deployment = list.items.find{r -> r.kind == "Deployment"}

    //Check that ti contains a Deployment named after the project
    assert deployment != null
    assert deployment.metadata.name == "kubernetes-with-client"

    ServiceAccount serviceAccount = list.items.find{r -> r.kind == "ServiceAccount"}
    assert serviceAccount != null
    assert serviceAccount.metadata.name == "kubernetes-with-client"

    RoleBinding rolebinding = list.items.find{r -> r.kind == "RoleBinding"}
    assert rolebinding != null
    assert rolebinding.metadata.name == "kubernetes-with-client:view"
}
