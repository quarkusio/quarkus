import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.kubernetes.api.model.apps.Deployment;

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
    //assert deployment != null
    assert deployment.metadata.name == "kubernetes-docker-build-and-deploy-deployment"
}
