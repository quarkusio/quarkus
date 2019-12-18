import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.Container
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.knative.serving.v1alpha1.Service;

//Check that file exits
String base = basedir
File knativeYml = new File(base, "target/kubernetes/knative.yml")
assert knativeYml.exists()

//Check that its parse-able
knativeYml.withInputStream { stream ->
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
}
// Un-comment once: https://github.com/fabric8io/kubernetes-client/issues/1838 is addressed.
//Check that ti contains a DeploymentConfig named after the project
//Service service = list.items.find{r -> r.kind == "Service"}
//assert service != null

//Container container = service.spec.runLatest.configuration.revisionTemplate.spec.container
//assert container != null

//assert container.image.startsWith("dev.local");
