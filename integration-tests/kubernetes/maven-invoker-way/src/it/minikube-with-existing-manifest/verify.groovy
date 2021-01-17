import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.kubernetes.api.model.Container
import io.dekorate.deps.kubernetes.api.model.Service
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/minikube.yml")
assert kubernetesYml.exists()
kubernetesYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null

    //Check that it contains the existing Service provided
    Service service = list.items.find{ r -> r.kind == "Service" && r.metadata.name == "postgres"}
    assert service != null
    assert service.metadata.labels.get("app.kubernetes.io/name") == "postgres"
    assert service.metadata.labels.get("app.kubernetes.io/version") == "10.6"
    assert service.spec.selector.get("app.kubernetes.io/name") == "postgres"
    assert service.spec.selector.get("app.kubernetes.io/version") == "10.6"

    //Check that it contains the existing Deployment with provided labels and selector
    Deployment deploymentProvided = list.items.find{r -> r.kind == "Deployment" && r.metadata.name == "postgres"}
    assert deploymentProvided != null
    assert deploymentProvided.metadata.labels.get("app.kubernetes.io/name") == "postgres"
    assert deploymentProvided.metadata.labels.get("app.kubernetes.io/version") == "10.6"
    assert deploymentProvided.spec.replicas == 1
    
    assert deploymentProvided.spec.selector.matchLabels.get("app.kubernetes.io/name") == "postgres"
    assert deploymentProvided.spec.selector.matchLabels.get("app.kubernetes.io/version") == "10.6"

    Container container = deploymentProvided.spec.template.spec.containers[0]
    assert container != null
    assert container.name == "postgres"
    assert container.ports.find{p -> p.protocol = "TCP"}.containerPort == 5432

    //Check that it created the application's Deployment with right labels and selector
    Deployment deployment = list.items.find{r -> r.kind == "Deployment" && r.metadata.name == "minikube-with-existing-manifest"}
    assert deployment != null
    assert deployment.metadata.labels.get("app.kubernetes.io/name") == "minikube-with-existing-manifest"
    assert deployment.metadata.labels.get("app.kubernetes.io/version") == "0.1-SNAPSHOT"
    assert deployment.spec.replicas == 1

    assert deployment.spec.selector.matchLabels.get("app.kubernetes.io/name") == "minikube-with-existing-manifest"
    assert deployment.spec.selector.matchLabels.get("app.kubernetes.io/version") == "0.1-SNAPSHOT"
}
