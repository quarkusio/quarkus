import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.KubernetesList
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;
import io.dekorate.deps.openshift.api.model.*

//Check that file exits
String base = basedir
File openshiftYml = new File(base, "target/kubernetes/openshift.yml")
assert openshiftYml.exists()
openshiftYml.withInputStream { stream ->
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    
    ImageStream imageStream = list.items.find{r -> r.kind == "ImageStream"}

    assert imageStream != null
    assert imageStream.spec.dockerImageRepository == "docker.io/test/openshift-docker-build-and-deploy"
}
