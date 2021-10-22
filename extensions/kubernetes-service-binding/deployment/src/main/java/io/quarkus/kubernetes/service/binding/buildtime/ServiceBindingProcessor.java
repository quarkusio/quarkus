
package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class ServiceBindingProcessor {

    @BuildStep
    public List<DecoratorBuildItem> createServiceBindingDecorators(KubernetesServiceBindingConfig serviceBindingConfig,
            List<KubernetesResourceMetadataBuildItem> resources) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        resources.stream()
                .distinct()
                .forEach(r -> {
                    if (!serviceBindingConfig.services.isEmpty()) {
                        result.add(new DecoratorBuildItem(r.getTarget(), new AddServiceBindingResourceDecorator(r.getGroup(),
                                r.getVersion(), r.getKind(), r.getName(), serviceBindingConfig)));
                    }
                });
        return result;
    }
}
