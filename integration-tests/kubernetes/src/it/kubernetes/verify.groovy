import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()

//Check that its parse-able
KubernetesList list = Serialization.unmarshal(kubernetesYml.text, KubernetesList.class)
assert list != null
Deployment deployment = list.items.find{r -> r.kind == "Deployment"}

//Check that ti contains a Deployment named after the project
assert deployment != null
assert deployment.metadata.name == "kubernetes"
