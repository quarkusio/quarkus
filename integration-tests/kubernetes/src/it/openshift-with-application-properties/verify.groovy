import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.*
import io.dekorate.deps.openshift.api.model.DeploymentConfig;

//Check that file exits
String base = basedir
File openshiftYml = new File(base, "target/kubernetes/openshift.yml")
assert openshiftYml.exists()

openshiftYml.withInputStream { stream ->
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null

    //Check that ti contains a Deployment named after the project
    DeploymentConfig deployment = list.items.find{r -> r.kind == "DeploymentConfig"}
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

    //Check the Service
    Service service = list.items.find{r -> r.kind == "Service"}
    assert service != null
    assert service.spec.ports[0].port == 9090
}
