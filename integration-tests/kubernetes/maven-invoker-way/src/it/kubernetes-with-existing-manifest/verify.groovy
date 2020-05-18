import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.kubernetes.api.model.EnvVar
import io.dekorate.deps.kubernetes.api.model.Container
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

    //Check that ti contains a Deployment named after the project
    assert deployment != null
    assert deployment.metadata.name == "kubernetes-with-existing-manifest"
    assert deployment.metadata.labels.get("app") == "quickstart"
    assert deployment.spec.replicas == 3

    Container container = deployment.spec.template.spec.containers[0]
    assert container != null
    assert container.name == "kubernetes-with-existing-manifest"

    EnvVar env = container.env.find{e -> e.name == "FOO"}
    assert env != null
    assert env.value == "BAR"

    assert container.ports.find{p -> p.name = "http"}.containerPort == 8080

}
