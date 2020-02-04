import io.dekorate.utils.Serialization
import io.dekorate.deps.kubernetes.api.model.*
import io.dekorate.deps.kubernetes.api.model.apps.Deployment;

//Check that file exits
String base = basedir

File openshiftYml = new File(base, "target/kubernetes/openshift.yml")
assert openshiftYml.exists()

openshiftYml.withInputStream { stream ->
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    list.items.each {
        assert it.metadata.name == "ofoo" || it.metadata.name == "s2i-java"
        assert it.metadata.labels.get("app") == "ofoo"
        assert it.metadata.labels.get("version") == "1.0-openshift"

    }
}

File kubernetesYml = new File(base, "target/kubernetes/kubernetes.yml")
assert kubernetesYml.exists()

kubernetesYml.withInputStream { stream -> 
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null
    list.items.each {
        assert it.metadata.name == "foo"
        assert it.metadata.labels.get("app") == "foo"
        assert it.metadata.labels.get("version") == "1.0-kube"

    }
    Deployment deployment = list.items.find{r -> r.kind == "Deployment"}
    assert deployment != null
}
