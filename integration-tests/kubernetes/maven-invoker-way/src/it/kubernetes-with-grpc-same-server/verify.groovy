import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.ContainerPort
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServicePort
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

    //Check that ti contains the Deployment with the expected ports
    assert deployment != null
    assert deployment.metadata.name == "kubernetes-with-grpc-same-server"
    List<ContainerPort> containerPorts = deployment.spec.template.spec.containers.get(0).ports
    assert containerPorts.stream().anyMatch(p -> p.getName() == "http" && p.containerPort == 8080);
    assert containerPorts.stream().noneMatch(p -> p.getName() == "grpc");

    Service service = list.items.find{ r -> r.kind == "Service"}

    //Check that ti contains the Service with the expected ports
    assert service != null
    assert service.metadata.name == "kubernetes-with-grpc-same-server"
    List<ServicePort> servicePorts = service.spec.ports
    assert servicePorts.stream().anyMatch(p -> p.getName() == "http" && p.targetPort.intVal == 8080);
    assert servicePorts.stream().noneMatch(p -> p.getName() == "grpc");
}
