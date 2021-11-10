
package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.List;
import java.util.Optional;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.servicebinding.model.ServiceBindingBuilder;
import io.dekorate.servicebinding.model.ServiceBindingSpecBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;

public class AddServiceBindingResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String group;
    private final String version;
    private final String kind;
    private final String name;
    private final KubernetesServiceBindingConfig config;
    private final List<KubernetesServiceBindingBuildItem> services;

    public AddServiceBindingResourceDecorator(String group, String version, String kind, String name,
            KubernetesServiceBindingConfig config,
            List<KubernetesServiceBindingBuildItem> services) {
        this.group = group;
        this.version = version;
        this.kind = kind;
        this.name = name;
        this.config = config;
        this.services = services;
    }

    @Override
    public void visit(KubernetesListFluent<?> list) {
        if (services.isEmpty()) {
            return;
        }

        ServiceBindingSpecBuilder spec = new ServiceBindingSpecBuilder();
        spec.withNewApplication()
                .withGroup(group)
                .withVersion(version)
                .withKind(kind)
                .withName(name)
                .endApplication()
                .withBindAsFiles(config.bindAsFiles)
                .withMountPath(config.mountPath.orElse(null));

        for (KubernetesServiceBindingBuildItem service : services) {
            String group = service.getApiVersion().contains("/")
                    ? Optional.ofNullable(service.getApiVersion()).map(a -> a.split("/")[0]).orElse(null)
                    : "";
            String version = service.getApiVersion().contains("/")
                    ? Optional.ofNullable(service.getApiVersion()).map(a -> a.split("/")[1]).orElse(null)
                    : service.getApiVersion();

            spec = spec.addNewService()
                    .withGroup(group)
                    .withVersion(version)
                    .withKind(service.getKind())
                    .withName(service.getName())
                    .endService();
        }

        ServiceBindingBuilder binding = new ServiceBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withSpec(spec.build());

        list.addToItems(binding.build());
    }
}
