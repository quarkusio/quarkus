
package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class RemoveDeploymentConfigResourceDecorator extends Decorator<KubernetesListBuilder> {
    private String name;

    public RemoveDeploymentConfigResourceDecorator(String name) {
        this.name = name;
    }

    @Override
    public void visit(KubernetesListBuilder builder) {
        List<HasMetadata> imageStreams = builder.getItems().stream()
                .filter(d -> d instanceof HasMetadata)
                .map(d -> (HasMetadata) d)
                .filter(i -> i.getKind().equals("DeploymentConfig") && i.getMetadata().getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());

        builder.removeAllFromItems(imageStreams);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }
}
