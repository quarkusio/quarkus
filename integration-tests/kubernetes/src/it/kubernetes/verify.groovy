import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;
import io.dekorate.deps.kubernetes.api.model.Service;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()
kubernetesYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)

    assert list != null
    Deployment deployment = list.items.find{r -> r.kind == "Deployment"}
    Service service = list.items.find{r -> r.kind == "Service"}

    //Check that it contains a Deployment named after the project
    assert deployment != null
    assert deployment.metadata.name == "kubernetes"

    //Check the port of the Deployment
    assert deployment.spec.template.spec.containers[0].ports[0].containerPort == 8080

    //Check that it contains a Service named after the project
    assert service != null
    assert service.metadata.name == "kubernetes"

    //Check the port of the Service
    assert service.spec.ports[0].port == 8080


}
