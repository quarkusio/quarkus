package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.KubernetesListBuilder;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;

public class RemoveBuilderImageResourceDecorator extends Decorator<KubernetesListBuilder> {

    private String name;

    public RemoveBuilderImageResourceDecorator(String name) {
        this.name = name;
    }

    @Override
    public void visit(KubernetesListBuilder builder) {
        builder.removeMatchingFromImageStreamItems(b -> b.build().getMetadata().getName().equalsIgnoreCase(name));
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddBuilderImageStreamResourceDecorator.class };
    }
}
