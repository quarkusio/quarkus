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

    //Check that ti contains a DeploymentConfig named after the project
    DeploymentConfig deployment = list.items.find{r -> r.kind == "DeploymentConfig"}
    assert deployment != null

    Container container = deployment.spec.template.spec.containers[0]
    assert container != null

    EnvVar envVar = container.env.find{e -> e.name == "JAVA_APP_JAR"}
    assert envVar != null
    assert envVar.value.endsWith("-runner.jar")
}

