import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()
kubernetesYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null

    StatefulSet statefulSet = list.items.find{ r -> r.kind == "StatefulSet"}

    //Check that it contains a StatefulSet named after the project
    assert statefulSet != null
    assert statefulSet.metadata.name == "kubernetes-docker-build-and-deploy-statefulset"
}
