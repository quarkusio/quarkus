
package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class ServiceBindingProcessor {

    @BuildStep
    public List<KubernetesServiceBindingBuildItem> createServiceBindingDecorators(
            KubernetesServiceBindingConfig serviceBindingConfig) {
        return serviceBindingConfig.services.entrySet().stream()
                .map(e -> new KubernetesServiceBindingBuildItem(e.getValue().apiVersion, e.getValue().kind,
                        e.getValue().name.orElse(e.getKey())))
                .collect(Collectors.toList());
    }

    @BuildStep
    public List<DecoratorBuildItem> createServiceBindingDecorators(KubernetesServiceBindingConfig serviceBindingConfig,
            List<KubernetesServiceBindingBuildItem> services, List<KubernetesResourceMetadataBuildItem> resources) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        resources.stream()
                .distinct()
                .forEach(r -> {
                    if (!serviceBindingConfig.services.isEmpty()) {
                        result.add(new DecoratorBuildItem(r.getTarget(), new AddServiceBindingResourceDecorator(r.getGroup(),
                                r.getVersion(), r.getKind(), r.getName(), serviceBindingConfig, services)));
                    }
                });
        return result;
    }
}
