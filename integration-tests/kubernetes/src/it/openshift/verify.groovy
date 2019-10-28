import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.*
import io.dekorate.deps.openshift.api.model.DeploymentConfig;

//Check that file exits
String base = basedir
File openshiftYml = new File(base, "target/kubernetes/openshift.yml")
assert openshiftYml.exists()

//Check that its parse-able
KubernetesList list = Serialization.unmarshal(openshiftYml.text, KubernetesList.class)
assert list != null

//Check that ti contains a DeploymentConfig named after the project
DeploymentConfig deployment = list.items.find{r -> r.kind == "DeploymentConfig"}
assert deployment != null

