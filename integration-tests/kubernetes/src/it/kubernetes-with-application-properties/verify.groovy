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
    assert deployment.metadata.name == "test-it"

    //Check that deployment has label foo/bar
    Map labels = deployment.metadata.labels
    assert labels.containsKey("foo")
    assert labels.get("foo") == "bar"

    //Check that container has an env var named some value.
    Container container = deployment.spec.template.spec.containers.get(0)
    EnvVar env = container.env.find{e -> e.name == "MY_ENV_VAR"}
    assert env != null
    assert env.value == "SOMEVALUE"

    //Check the image
    assert deployment.spec.template.spec.containers[0].image == "quay.io/grp/kubernetes-with-application-properties:0.1-SNAPSHOT"
    assert deployment.spec.template.spec.containers[0].ports[0].containerPort == 9090

    //Check the Service
    Service service = list.items.find{r -> r.kind == "Service"}
    assert service != null
    assert service.spec.ports[0].port == 9090
}
