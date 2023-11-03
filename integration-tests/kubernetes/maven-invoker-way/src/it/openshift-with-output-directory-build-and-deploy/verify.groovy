import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.openshift.api.model.*

//Check that file exits
String base = basedir
File openshiftYml = new File(base, "woo/openshift.yml")
assert openshiftYml.exists()
openshiftYml.withInputStream { stream ->
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    
    BuildConfig buildConfig = list.items.find{r -> r.kind == "BuildConfig"}

    //Check that ti contains a Deployment named after the project
    assert buildConfig != null
    assert buildConfig.metadata.name == "openshift-with-output-directory-build-and-deploy"
}
