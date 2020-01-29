import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.*
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()

kubernetesYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    Deployment deployment = list.items.find{r -> r.kind == "Deployment"}
    assert deployment != null
    String group = deployment.metadata.labels.get("group")
    assert group == "sys"
}
