import io.dekorate.utils.Serialization
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.KubernetesList

//Check that file exits
String base = basedir
File knativeYml = new File(base, "target/kubernetes/knative.yml")
assert knativeYml.exists()
knativeYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    
    Service service = list.items.find{r -> r.kind == "Service"}

    //Check that ti contains a Service named after the project
    assert service != null
    assert service.metadata.name == "knative-jib-build-and-deploy"
}
