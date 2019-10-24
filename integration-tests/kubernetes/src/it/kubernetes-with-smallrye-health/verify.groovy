import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.*
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()

//Check that its parse-able
KubernetesList list = Serialization.unmarshal(kubernetesYml.text, KubernetesList.class)
assert list != null

//Check that ti contains a Deployment named after the project
Deployment deployment = list.items.find{r -> r.kind == "Deployment"}
assert deployment != null
assert deployment.metadata.name == "kubernetes-with-smallrye-health"

//Check that container has probes pointing to smallrye-health endpoint
Container container = deployment.spec.template.spec.containers.get(0)
assert container != null
Probe readiness = container.readinessProbe
assert readiness != null
assert readiness.httpGet.path == "/health/ready"
Probe liveness = container.livenessProbe
assert liveness != null
assert liveness.httpGet.path == "/health/live"
