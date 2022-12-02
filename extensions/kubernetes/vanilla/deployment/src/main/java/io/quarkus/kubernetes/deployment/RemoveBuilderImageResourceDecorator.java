package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.ImageStream;

public class RemoveBuilderImageResourceDecorator extends Decorator<KubernetesListBuilder> {

    private String name;

    public RemoveBuilderImageResourceDecorator(String name) {
        this.name = name;
    }

    @Override
    public void visit(KubernetesListBuilder builder) {
        List<HasMetadata> imageStreams = builder.getItems().stream()
                .filter(i -> i instanceof ImageStream)
                .map(i -> (HasMetadata) i)
                .filter(i -> i.getMetadata().getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());

        builder.removeAllFromItems(imageStreams);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddBuilderImageStreamResourceDecorator.class };
    }
}
